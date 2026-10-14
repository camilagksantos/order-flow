import { Address } from './address';
import { CustomerStatus } from './customer-status';

export type Customer = {
    id: number;
    name: string;
    email: string;
    nif: string;
    phone: string | null;
    status: CustomerStatus;
    addresses: Address[];
};