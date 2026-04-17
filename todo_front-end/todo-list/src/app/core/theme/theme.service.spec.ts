import { TestBed } from '@angular/core/testing';
import { DOCUMENT } from '@angular/common';
import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  let document: Document;

  beforeEach(() => {
    localStorage.clear();
    window.history.replaceState({}, '', '/login');

    TestBed.configureTestingModule({});
    document = TestBed.inject(DOCUMENT);
    delete document.documentElement.dataset['theme'];
    delete document.body.dataset['theme'];
  });

  afterEach(() => {
    localStorage.clear();
    window.history.replaceState({}, '', '/login');
  });

  it('forces the light theme on public routes even when dark is stored', () => {
    localStorage.setItem('todo-list-theme', 'dark');

    const service = TestBed.inject(ThemeService);
    service.applyRouteTheme('/register');

    expect(document.documentElement.dataset['theme']).toBe('light');
    expect(document.body.dataset['theme']).toBe('light');
    expect(service.isDarkTheme()).toBeFalse();
  });

  it('applies the stored preferred theme inside the authenticated area', () => {
    localStorage.setItem('todo-list-theme', 'dark');

    const service = TestBed.inject(ThemeService);
    service.applyRouteTheme('/tasks');

    expect(document.documentElement.dataset['theme']).toBe('dark');
    expect(document.body.dataset['theme']).toBe('dark');
    expect(service.isDarkTheme()).toBeTrue();
  });
});
