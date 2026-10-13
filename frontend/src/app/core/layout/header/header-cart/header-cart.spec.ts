import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HeaderCart } from './header-cart';

describe('HeaderCart', () => {
  let component: HeaderCart;
  let fixture: ComponentFixture<HeaderCart>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HeaderCart],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(HeaderCart);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should link to the cart page', () => {
    const link = fixture.nativeElement.querySelector('a') as HTMLAnchorElement;

    expect(link.getAttribute('href')).toBe('/cart');
  });

  it('should have an accessible label', () => {
    const link = fixture.nativeElement.querySelector('a') as HTMLAnchorElement;

    expect(link.getAttribute('aria-label')).toBe('Carrinho');
  });
});