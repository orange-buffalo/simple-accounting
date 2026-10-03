import { mount, flushPromises } from '@vue/test-utils';
import { nextTick, ref } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import SaFormCountryInput from '@/components/form/SaFormCountryInput.vue';

const { loadCountries, formApi } = vi.hoisted(() => ({
  loadCountries: vi.fn(),
  formApi: { formValues: { value: {} as Record<string, unknown> } },
}));

vi.mock('@/services/api/use-gql-api', () => ({ useLazyQuery: () => loadCountries }));
vi.mock('@/components/form/sa-form-components-api', () => ({ useSaFormComponentsApi: () => formApi }));
vi.mock('@/services/i18n/countries', () => ({ getCountryName: (code: string) => code }));

const createInput = (currency?: string) => mount(SaFormCountryInput, {
  props: { prop: 'residency', currency },
  global: {
    stubs: {
      SaFormSelect: { props: ['disabled'], template: '<select :disabled="disabled"><slot /></select>' },
      ElOption: { props: ['value', 'label'], template: '<option :value="value">{{ label }}</option>' },
    },
  },
});

describe('currency-restricted residency', () => {
  beforeEach(() => {
    loadCountries.mockReset();
    formApi.formValues = ref({ residency: null }) as typeof formApi.formValues;
  });

  it('disables residency without currency and does not load countries', () => {
    const wrapper = createInput();
    expect(wrapper.find('select').attributes('disabled')).toBeDefined();
    expect(loadCountries).not.toHaveBeenCalled();
  });

  it('preserves a compatible loaded residency and clears it on an incompatible currency change', async () => {
    formApi.formValues.value.residency = 'AU';
    loadCountries.mockResolvedValueOnce(['AU']).mockResolvedValueOnce(['US', 'PA']);
    const wrapper = createInput('AUD');
    expect(wrapper.find('select').attributes('disabled')).toBeDefined();
    await flushPromises();
    expect(formApi.formValues.value.residency).toBe('AU');
    expect(wrapper.find('select').attributes('disabled')).toBeUndefined();
    await wrapper.setProps({ currency: 'USD' });
    await flushPromises();
    expect(loadCountries).toHaveBeenLastCalledWith({ currency: 'USD' });
    expect(formApi.formValues.value.residency).toBeNull();
    expect(wrapper.findAll('option').map((option) => option.text())).toEqual(['PA', 'US']);
    await wrapper.setProps({ currency: '' });
    expect(wrapper.find('select').attributes('disabled')).toBeDefined();
    expect(wrapper.findAll('option')).toHaveLength(0);
  });

  it('preserves residency when it is allowed for both currencies', async () => {
    formApi.formValues.value.residency = 'AU';
    loadCountries.mockResolvedValueOnce(['AU']);
    let resolveCountries!: (countries: string[]) => void;
    loadCountries.mockImplementationOnce(() => new Promise<string[]>((resolve) => { resolveCountries = resolve; }));
    const wrapper = createInput('AUD');
    await flushPromises();
    await wrapper.setProps({ currency: 'USD' });
    expect(formApi.formValues.value.residency).toBe('AU');
    expect(wrapper.find('select').attributes('disabled')).toBeDefined();
    resolveCountries(['AU', 'US']);
    await flushPromises();
    expect(loadCountries).toHaveBeenLastCalledWith({ currency: 'USD' });
    expect(formApi.formValues.value.residency).toBe('AU');
    expect(wrapper.find('select').attributes('disabled')).toBeUndefined();
    expect(wrapper.findAll('option').map((option) => option.text())).toEqual(['AU', 'US']);
  });

  it('clears an incompatible existing residency', async () => {
    formApi.formValues.value.residency = 'AU';
    loadCountries.mockResolvedValue(['US']);
    createInput('USD');
    await flushPromises();
    expect(formApi.formValues.value.residency).toBeNull();
  });

  it('ignores a stale response after currency changes', async () => {
    let resolveAud!: (countries: string[]) => void;
    loadCountries.mockImplementationOnce(() => new Promise<string[]>((resolve) => { resolveAud = resolve; }));
    loadCountries.mockResolvedValueOnce(['US']);
    const wrapper = createInput('AUD');
    await wrapper.setProps({ currency: 'USD' });
    await flushPromises();
    formApi.formValues.value.residency = 'US';
    resolveAud(['AU']);
    await flushPromises();
    await nextTick();
    expect(wrapper.findAll('option').map((option) => option.text())).toEqual(['US']);
    expect(formApi.formValues.value.residency).toBe('US');
  });

  it('invalidates residency after an active request fails and reports the error', async () => {
    const error = new Error('Slurm delivery connection lost');
    const errorHandler = vi.fn();
    formApi.formValues.value.residency = 'AU';
    loadCountries.mockResolvedValueOnce(['AU']).mockRejectedValueOnce(error);
    const wrapper = mount(SaFormCountryInput, {
      props: { prop: 'residency', currency: 'AUD' },
      global: {
        config: { errorHandler },
        stubs: {
          SaFormSelect: { props: ['disabled'], template: '<select :disabled="disabled"><slot /></select>' },
          ElOption: { props: ['value', 'label'], template: '<option :value="value">{{ label }}</option>' },
        },
      },
    });
    await flushPromises();
    await wrapper.setProps({ currency: 'USD' });
    await flushPromises();
    expect(formApi.formValues.value.residency).toBeNull();
    expect(wrapper.find('select').attributes('disabled')).toBeDefined();
    expect(wrapper.findAll('option')).toHaveLength(0);
    expect(errorHandler.mock.calls[0][0]).toBe(error);
    loadCountries.mockResolvedValueOnce(['AU']);
    await wrapper.setProps({ currency: 'AUD' });
    await flushPromises();
    expect(wrapper.find('select').attributes('disabled')).toBeUndefined();
    expect(formApi.formValues.value.residency).toBeNull();
  });

  it('ignores a stale request failure after a newer request succeeds', async () => {
    let rejectAud!: (error: Error) => void;
    loadCountries.mockImplementationOnce(() => new Promise<string[]>((resolve, reject) => { rejectAud = reject; }));
    loadCountries.mockResolvedValueOnce(['US']);
    const wrapper = createInput('AUD');
    await wrapper.setProps({ currency: 'USD' });
    await flushPromises();
    formApi.formValues.value.residency = 'US';
    rejectAud(new Error('Outdated Slurm delivery connection lost'));
    await flushPromises();
    expect(formApi.formValues.value.residency).toBe('US');
    expect(wrapper.find('select').attributes('disabled')).toBeUndefined();
    expect(wrapper.findAll('option').map((option) => option.text())).toEqual(['US']);
  });
});
