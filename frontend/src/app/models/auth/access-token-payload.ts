export type AccessTokenPayload = {
    sub: string;
    roles: string[];
    customerId?: number;
};