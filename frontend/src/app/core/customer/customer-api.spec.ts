import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { Customer } from '../../models/customer/customer';
import { CustomerApi } from './customer-api';

describe('CustomerApi', () => {
  let api: CustomerApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(CustomerApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should get the customer by id', () => {
    const customer: Customer = {
      id: 7,
      name: 'Ana Costa',
      email: 'ana@exemplo.pt',
      nif: '123456789',
      phone: null,
      status: 'ACTIVE',
      addresses: [],
    };
    let received: unknown;

    api.findById(7).subscribe((value) => (received = value));

    const req = http.expectOne(`${environment.apiUrl}/api/v1/customers/7`);
    expect(req.request.method).toBe('GET');
    req.flush(customer);
    expect(received).toEqual(customer);
  });
});