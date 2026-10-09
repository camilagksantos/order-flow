import { ComponentFixture, TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { Toaster } from '../toaster';
import { ToastContainer } from './toast-container';

describe('ToastContainer', () => {
  let component: ToastContainer;
  let fixture: ComponentFixture<ToastContainer>;
  let toaster: Toaster;
  let host: HTMLElement;

  const render = async () => {
    fixture.detectChanges();
    await fixture.whenStable();
  };

  const toastElements = () =>
    Array.from(host.querySelectorAll<HTMLElement>('[role="alert"], [role="status"]'));

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToastContainer],
    }).compileComponents();

    fixture = TestBed.createComponent(ToastContainer);
    component = fixture.componentInstance;
    toaster = TestBed.inject(Toaster);
    host = fixture.nativeElement;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render nothing when there are no toasts', () => {
    expect(toastElements()).toHaveLength(0);
  });

  it('should render an error toast as an alert with its message', async () => {
    toaster.error('Palavra-passe incorreta');
    await render();

    const elements = toastElements();
    expect(elements).toHaveLength(1);
    expect(elements[0].getAttribute('role')).toBe('alert');
    expect(elements[0].textContent).toContain('Palavra-passe incorreta');
  });

  it('should render a success toast as a status', async () => {
    toaster.success('Pedido confirmado');
    await render();

    expect(toastElements()[0].getAttribute('role')).toBe('status');
  });

  it('should render the newest toast first', async () => {
    toaster.info('primeiro');
    toaster.success('segundo');
    await render();

    const elements = toastElements();
    expect(elements[0].textContent).toContain('segundo');
    expect(elements[1].textContent).toContain('primeiro');
  });

  it('should dismiss a toast when the close button is clicked', async () => {
    toaster.info('um aviso');
    await render();

    host.querySelector<HTMLButtonElement>('button[aria-label="Fechar notificação"]')!.click();

    expect(toaster.toasts()).toHaveLength(0);
  });

  it('should pause the countdown on hover and resume on leave', async () => {
    const pause = vi.spyOn(toaster, 'pause');
    const resume = vi.spyOn(toaster, 'resume');
    toaster.error('falhou');
    await render();
    const id = toaster.toasts()[0].id;

    toastElements()[0].dispatchEvent(new Event('mouseenter'));
    expect(pause).toHaveBeenCalledWith(id);

    toastElements()[0].dispatchEvent(new Event('mouseleave'));
    expect(resume).toHaveBeenCalledWith(id);
  });
});