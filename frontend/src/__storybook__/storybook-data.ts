import type {
  CategoryDto, CustomerDto, DocumentDto, GeneralTaxDto,
} from '@/services/api';
import { onGetToDefaultWorkspacePath, pageResponseRaw } from '@/__storybook__/api-mocks';

const generalTaxes = {
  planetExpressTax: {
    id: 13,
    title: 'Planet Express Tax',
    rateInBps: 1100,
  } as GeneralTaxDto,
};

const customers = {
  governmentOfEarth: {
    id: 77,
    name: 'Government of Earth',
  } as CustomerDto,
  democraticOrderOfPlanets: {
    id: 42,
    name: 'Democratic Order of Planets',
  } as CustomerDto,
};

const categories = {
  planetExpressCategory: {
    id: 101,
    name: 'PlanetExpress',
    expense: true,
    income: true,
  } as CategoryDto,
  slurmCategory: {
    id: 102,
    name: 'Slurm',
    expense: true,
    income: false,
  } as CategoryDto,
};

const documents = {
  lunaParkDeliveryAgreement: {
    id: 201,
    version: 0,
    name: 'Luna Park Delivery.pdf',
    timeUploaded: new Date('2020-03-08'),
    sizeInBytes: 64422,
  } as DocumentDto,
  cheesePizzaAndALargeSodaReceipt: {
    id: 202,
    version: 0,
    name: 'Cheese Pizza And A Large Soda.doc',
    timeUploaded: new Date('2021-04-08'),
    sizeInBytes: 8222,
  } as DocumentDto,
  coffeeReceipt: {
    id: 203,
    version: 0,
    name: '100_cups.xlsx',
    timeUploaded: new Date('2020-06-03'),
    sizeInBytes: 984843,
  } as DocumentDto,
};

export const storybookData = {
  generalTaxes,
  customers,
  categories,
  documents,
  mockApi: () => {
    Object.values(generalTaxes)
      .forEach((generalTax) => onGetToDefaultWorkspacePath(`/general-taxes/${generalTax.id}`, generalTax));
    Object.values(customers)
      .forEach((customer) => onGetToDefaultWorkspacePath(`/customers/${customer.id}`, customer));
    onGetToDefaultWorkspacePath('/customers', pageResponseRaw(Object.values(customers)));
    onGetToDefaultWorkspacePath('/categories', pageResponseRaw(Object.values(categories)));
  },
  storyComponentConfig: {
    setup: () => ({
      storybookData,
    }),
    beforeCreate() {
      storybookData.mockApi();
    },
  },
};
