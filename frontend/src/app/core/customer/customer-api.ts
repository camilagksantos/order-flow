import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { environment } from '../../../environments/environment';
import { Customer } from '../../models/customer/customer';
import { Observable } from 'rxjs';

@Service()
export class CustomerApi {
    private readonly http = inject(HttpClient);
    private readonly baseUrl = `${environment.apiUrl}/api/v1/customers`;

    findById(id: number): Observable<Customer> {
        return this.http.get<Customer>(`${this.baseUrl}/${id}`);
    }
}
