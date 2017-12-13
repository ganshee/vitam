import { Injectable } from '@angular/core';
import {CookieService} from "angular2-cookie/core";
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {Router} from '@angular/router';
import {Observable} from "rxjs/Observable";

const TENANT_COOKIE = 'tenant';
const BASE_URL = '/ihm-recette/v1/api/';
const TENANTS = 'tenants';

@Injectable()
export class ResourcesService {

  constructor(private cookies: CookieService, private http: HttpClient, private router: Router) { }

  get(url, header?: HttpHeaders, responsetype?: any): Observable<any> {
    const options: any = {};

    if (!header) {
      header = new HttpHeaders();
    }
    if ( this.getTenant()) {
      header = header.set('X-Tenant-Id', this.getTenant());
    }

    options.headers = header;

    if (responsetype && responsetype != 'json') {
      options.responseType = responsetype;
      options.observe = 'response';
    } else {
      options.responseType = 'json';
    }

    return this.http.get(`${BASE_URL}${url}`, options);
  }

  post(url, header?: HttpHeaders, body?: any, responsetype?: any): Observable<any> {
    const options: any = {};
    if (!header) {
      header = new HttpHeaders();
    }
    if (this.getTenant()) {
      header = header.set('X-Tenant-Id', this.getTenant());
    }
    options.headers = header;
    options.responseType = responsetype || 'json';
    return this.http.post(`${BASE_URL}${url}`, body, options);
  }

  delete(url) {
    const options: any = {};
    let headers: HttpHeaders = new HttpHeaders();
    if ( this.getTenant()) {
      headers = headers.set('X-Tenant-Id', this.getTenant());
    }
    options.headers = headers;
    return this.http.delete(`${BASE_URL}${url}`, options);
  }

  getTenants() {
    return this.get(TENANTS);
  }

  setTenant(tenantId: string) {
    this.cookies.put(TENANT_COOKIE, tenantId);
  }

  getTenant() {
    return this.cookies.get(TENANT_COOKIE);
  }

}
