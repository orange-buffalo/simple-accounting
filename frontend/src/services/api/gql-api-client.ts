import { AnyVariables, Client, fetchExchange, DocumentInput, OperationContext, OperationResult } from '@urql/core';

import { graphql } from '@/services/api/gql';
import { updateApiToken, useAuth } from '@/services/api/auth.ts';
import { authExchange } from '@urql/exchange-auth';
import { jwtDecode } from 'jwt-decode';
import {
  ApiAuthError, ApiBusinessError, ApiError, ApiFieldLevelValidationError, ClientApiError, FieldError,
} from '@/services/api/api-errors.ts';
import { SaGrapQlErrorType, ValidationErrorDetails } from '@/services/api/gql/graphql.ts';

const refreshTokenMutation = graphql(/* GraphQL */ `
    mutation refreshAccessToken {
        refreshAccessToken {
            accessToken
        }
    }
`);

const getJwtToken = () => {
  const { getToken } = useAuth();
  return getToken();
};

const jwtAuthExchange = authExchange(async utils => {
  // noinspection JSUnusedGlobalSymbols
  return {
    addAuthToOperation(operation) {
      const token = getJwtToken();
      if (!token) {
        return operation;
      }
      return utils.appendHeaders(operation, {
        Authorization: `Bearer ${token}`,
      });
    },

    didAuthError(error, _operation) {
      // see SaGrapQlException
      return error.graphQLErrors.some(e => e.extensions?.errorType === SaGrapQlErrorType.NotAuthorized);
    },

    async refreshAuth() {
      const result = await utils.mutate(refreshTokenMutation, {});
      const newAccessToken = result.data?.refreshAccessToken.accessToken;
      if (newAccessToken) {
        updateApiToken(newAccessToken);
        return;
      }
    },

    willAuthError() {
      const token = getJwtToken();
      if (!token) {
        return true;
      }
      const { exp } = jwtDecode(token) as {
        exp: number,
      };
      // give some leeway of 20 seconds
      return Date.now() >= (exp * 1000 - 20000);
    },
  };
});

const gqlNativeClient = new Client({
  url: '/api/graphql',
  exchanges: [jwtAuthExchange, fetchExchange],
  fetchOptions: {
    // to enable refresh token cookie
    credentials: 'include',
    // TODO add signal: AbortSignal.timeout(15000) when https://github.com/urql-graphql/urql/issues/3801 is fixed
  },
  preferGetMethod: false,
  requestPolicy: 'network-only',
});

export interface GrapQlClient {
  query<Data = any, Variables extends AnyVariables = AnyVariables>(
    query: DocumentInput<Data, Variables>,
    variables: Variables,
    context?: Partial<OperationContext>,
  ): Promise<Data>;

  mutation<Data = any, Variables extends AnyVariables = AnyVariables>(
    query: DocumentInput<Data, Variables>,
    variables: Variables,
    context?: Partial<OperationContext>,
  ): Promise<Data>;
}

function convertValidationErrorsToFieldErrors(
  validationErrors: ValidationErrorDetails[],
): FieldError[] {
  return validationErrors.map((validationError) => {
    const params: { [key: string]: string } | undefined = validationError.params
      ? Object.fromEntries(validationError.params.map((param) => [param.name, param.value]))
      : undefined;

    return {
      field: validationError.path,
      error: validationError.error,
      message: validationError.message,
      params: params && Object.keys(params).length > 0 ? params : undefined,
    };
  });
}

async function executeGqlRequestAndHandleErrors<Data>(
  operation: () => Promise<OperationResult<Data>>,
): Promise<Data> {
  const result = await operation();
  if (result.error) {
    if (result.error.networkError) {
      if (result.error.response.status === 401) {
        throw new ApiAuthError();
      }
      throw new ClientApiError(`Network error`, result.error.networkError);
    }
    if (result.error.graphQLErrors.length > 1) {
      throw new ApiError(
        `Multiple errors received, which is not supported: ${JSON.stringify(result.error.graphQLErrors)}`);
    }
    if (result.error.graphQLErrors.length === 0) {
      throw new ApiError('Unknown error');
    }
    const graphQLError = result.error.graphQLErrors[0];
    if (graphQLError.extensions?.errorType === SaGrapQlErrorType.NotAuthorized) {
      throw new ApiAuthError();
    }
    if (graphQLError.extensions?.errorType === SaGrapQlErrorType.FieldValidationFailure) {
      const validationErrors = graphQLError.extensions.validationErrors;
      if (!Array.isArray(validationErrors)) {
        throw new ApiError(`Invalid validation errors format: ${JSON.stringify(graphQLError)}`);
      }
      throw new ApiFieldLevelValidationError(
        convertValidationErrorsToFieldErrors(validationErrors as ValidationErrorDetails[]),
      );
    }
    if (graphQLError.extensions?.errorType === SaGrapQlErrorType.BusinessError) {
      const errorCode = graphQLError.extensions.errorCode;
      if (typeof errorCode !== 'string') {
        throw new ApiError(`Invalid business error format: ${JSON.stringify(graphQLError)}`);
      }
      const businessError = new ApiBusinessError({
        error: errorCode,
        message: graphQLError.message,
      });
      businessError.extensions = graphQLError.extensions;
      throw businessError;
    }

    throw new ApiError(
      `Unsupported error received: ${JSON.stringify(graphQLError)}`);
  }
  return result.data;
}

export const gqlClient: GrapQlClient = {
  async query(query, variables, context) {
    return executeGqlRequestAndHandleErrors(() => gqlNativeClient.query(query, variables, context)
      .toPromise());
  },

  async mutation(query, variables, context) {
    return executeGqlRequestAndHandleErrors(() => gqlNativeClient.mutation(query, variables, context)
      .toPromise());
  },
};
