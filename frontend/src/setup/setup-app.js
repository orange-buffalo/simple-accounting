import Vue from 'vue';
import {
  Pagination,
  Dialog,
  Menu,
  Input,
  InputNumber,
  Checkbox,
  Switch,
  Select,
  Option,
  OptionGroup,
  Button,
  Table,
  TableColumn,
  DatePicker,
  Tooltip,
  Form,
  FormItem,
  Progress,
  Steps,
  Step,
  Container,
  Header,
  Aside,
  Main,
  Footer,
  Message,
  MessageBox,
} from 'element-ui';
import ElementLocale from 'element-ui/lib/locale';
import VueCompositionApi from '@vue/composition-api';
import SimpleAccounting from '@/SimpleAccounting.vue';
import '@/styles/main.scss';
import setupRouter from '@/setup/setup-router';
import setupStore from '@/setup/setup-store';
import setupI18n from '@/setup/setup-i18n';
import { app } from '@/services/app-services';

function setupElementUi({ i18n }) {
  Vue.use(Pagination);
  Vue.use(Dialog);
  Vue.use(Menu);
  Vue.use(Input);
  Vue.use(InputNumber);
  Vue.use(Checkbox);
  Vue.use(Switch);
  Vue.use(Select);
  Vue.use(Option);
  Vue.use(OptionGroup);
  Vue.use(Button);
  Vue.use(Table);
  Vue.use(TableColumn);
  Vue.use(DatePicker);
  Vue.use(Tooltip);
  Vue.use(Form);
  Vue.use(FormItem);
  Vue.use(Progress);
  Vue.use(Steps);
  Vue.use(Step);
  Vue.use(Container);
  Vue.use(Header);
  Vue.use(Aside);
  Vue.use(Main);
  Vue.use(Footer);

  Vue.prototype.$message = Message;
  Vue.prototype.$confirm = MessageBox.confirm;

  ElementLocale.i18n((key, value) => i18n.t(key, value));
}

function setupApp() {
  Vue.use(VueCompositionApi);

  Vue.config.productionTip = false;
  const router = setupRouter();
  const store = setupStore();
  const i18n = setupI18n();
  const vue = new Vue({
    router,
    store,
    i18n,
    render: h => h(SimpleAccounting),
  });

  setupElementUi({ i18n });

  app.init({
    vue,
    router,
    store,
    i18n,
  });
}

function mountApp() {
  app.vue.$mount('#simple-accounting');
}

export default {
  app,
  setupApp,
  mountApp,
};
