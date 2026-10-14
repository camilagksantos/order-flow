export type Address = {
    id: number;
    street: string;
    number: string;
    complement: string | null;
    neighborhood: string;
    city: string;
    district: string;
    postalCode: string;
    country: string;
    defaultAddress: boolean;
};