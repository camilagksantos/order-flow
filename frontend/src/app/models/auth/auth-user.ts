export type AuthUser = {
    email: string;
    roles: string[];
    customerId: number | null;
};