import { TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { TitleStrategy, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { PageTitleStrategy } from './page-title-strategy';

describe('PageTitleStrategy', () => {
  let harness: RouterTestingHarness;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'products', title: 'Produtos', children: [] },
          { path: 'untitled', children: [] },
        ]),
        { provide: TitleStrategy, useClass: PageTitleStrategy },
      ],
    });
    harness = await RouterTestingHarness.create();
  });

  it('should add the brand name after the page title', async () => {
    await harness.navigateByUrl('/products');

    expect(TestBed.inject(Title).getTitle()).toBe('Produtos · Velora');
  });

  it('should use only the brand name when the route has no title', async () => {
    await harness.navigateByUrl('/untitled');

    expect(TestBed.inject(Title).getTitle()).toBe('Velora');
  });
});