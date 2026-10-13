import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HeaderBrand } from './header-brand';

describe('HeaderBrand', () => {
  let component: HeaderBrand;
  let fixture: ComponentFixture<HeaderBrand>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HeaderBrand],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(HeaderBrand);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show the brand name', () => {
    expect(fixture.nativeElement.textContent).toContain('Velora');
  });

  it('should link to the products page', () => {
    const link = fixture.nativeElement.querySelector('a') as HTMLAnchorElement;

    expect(link.getAttribute('href')).toBe('/products');
  });
});