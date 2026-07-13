import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { SessionService } from './session.service';

describe('SessionService', () => {
  let service: SessionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(SessionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the minimal session profile from the BFF', () => {
    const expectedUser = {
      username: 'home-banking-user',
      displayName: 'Federico'
    };

    service.load().subscribe((user) => expect(user).toEqual(expectedUser));

    const request = http.expectOne('/web/me');
    expect(request.request.method).toBe('GET');
    request.flush(expectedUser);

    expect(service.user()).toEqual(expectedUser);
  });

  it('revalidates the server session during client-side navigation', () => {
    const initialUser = { username: 'cliente', displayName: 'Cliente Nerva' };
    const refreshedUser = { username: 'cliente', displayName: 'Cliente Actualizado' };

    service.load().subscribe();
    http.expectOne('/web/me').flush(initialUser);

    service
      .load()
      .subscribe((serverUser) => expect(serverUser).toEqual(refreshedUser));
    http.expectOne('/web/me').flush(refreshedUser);

    expect(service.user()).toEqual(refreshedUser);
  });

  it('clears the local profile', () => {
    service.load().subscribe();
    http
      .expectOne('/web/me')
      .flush({ username: 'cliente', displayName: 'Cliente Nerva' });

    service.clear();

    expect(service.user()).toBeNull();
  });
});
