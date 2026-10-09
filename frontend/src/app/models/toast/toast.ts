import { ToastKind } from './toast-kind';

export type Toast = {
    id: number;
    kind: ToastKind;
    message: string;
    duration: number;
};