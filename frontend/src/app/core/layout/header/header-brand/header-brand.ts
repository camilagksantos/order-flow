import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { brandName } from '../../brand';

@Component({
  imports: [RouterLink],
  selector: 'app-header-brand',
  templateUrl: './header-brand.html',
})
export class HeaderBrand {
  protected readonly brandName = brandName;
}
