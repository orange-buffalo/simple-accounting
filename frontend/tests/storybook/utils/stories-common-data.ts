import { Components } from '@/services/api/api-client-definition';

const categories: Array<Components.Schemas.CategoryDto> = [];
for (let i = 1; i < 5; i += 1) {
  categories[i - 1] = {
    id: i,
    name: `Category ${i}`,
    version: 1,
    income: true,
    expense: true,
  };
}

export const Categories = {
  category1: categories[0],
  category2: categories[1],
  category3: categories[2],
  category4: categories[3],
};

const customers: Array<Components.Schemas.CustomerDto> = [];
for (let i = 1; i < 5; i += 1) {
  customers[i - 1] = {
    id: i,
    name: `Customer ${i}`,
    version: 1,
  };
}

export const Customers = {
  customer1: customers[0],
  customer2: customers[1],
  customer3: customers[2],
  customer4: customers[3],
};
