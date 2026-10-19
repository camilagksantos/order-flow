import { Injectable, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';
import { brandName } from './brand';

@Injectable()
export class PageTitleStrategy extends TitleStrategy {
    private readonly title = inject(Title);

    override updateTitle(snapshot: RouterStateSnapshot): void {
        const pageTitle = this.buildTitle(snapshot);
        this.title.setTitle(pageTitle ? `${pageTitle} · ${brandName}` : brandName);
    }
}