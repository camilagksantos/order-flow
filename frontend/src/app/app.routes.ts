import { Routes } from '@angular/router';
import { guestGuard } from './core/guard/guest-guard';
import { authGuard } from './core/guard/auth-guard';
import { notFoundGuard } from './core/guard/not-found-guard';


export const routes: Routes = [
    { path: '', pathMatch: 'full', redirectTo: 'products' },
    {
        path: 'products',
        title: 'Produtos',
        loadComponent: () => import('./features/catalog/product-list/product-list').then((m) => m.ProductList),
    },
    {
        path: 'login',
        title: 'Entrar',
        canActivate: [guestGuard],
        loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
    },
    {
        path: 'cart',
        title: 'Carrinho',
        canActivate: [authGuard],
        loadComponent: () => import('./features/cart/cart-page/cart-page').then((m) => m.CartPage),
    },
    {
        path: 'checkout',
        title: 'Finalizar compra',
        canActivate: [authGuard],
        loadComponent: () => import('./features/checkout/checkout-page/checkout-page').then((m) => m.CheckoutPage),
    },
    {
        path: 'orders',
        title: 'Meus pedidos',
        canActivate: [authGuard],
        loadComponent: () => import('./features/orders/order-list/order-list').then((m) => m.OrderList),
    },
    { path: '**', canActivate: [notFoundGuard], children: [] },
];