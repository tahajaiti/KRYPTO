import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { API_BASE_URL } from '../../config/api.config';

const ABSOLUTE_URL_PATTERN = /^https?:\/\//i;

export const apiBaseUrlInterceptor: HttpInterceptorFn = (request, next) => {
  if (ABSOLUTE_URL_PATTERN.test(request.url)) {
    return next(request);
  }

  const baseUrl = inject(API_BASE_URL).replace(/\/$/, '');
  const normalizedPath = request.url.startsWith('/') ? request.url : `/${request.url}`;

  return next(request.clone({ url: `${baseUrl}${normalizedPath}` }));
};
