import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { apiRoutes } from '../api/api-routes';
import { OperationStatusResponseDTO, ProfileDTO } from '../auth/auth.dtos';

export type UpdatePasswordPayload = {
  readonly currentPassword: string;
  readonly newPassword: string;
  readonly confirmPassword: string;
};

export type UpdatePhotoPayload = {
  readonly photoDataUrl: string | null;
};

@Injectable({
  providedIn: 'root',
})
export class ProfileApiService {
  private readonly http = inject(HttpClient);

  getProfile(): Observable<ProfileDTO> {
    return this.http.get<ProfileDTO>(apiRoutes.profile.base);
  }

  updatePassword(payload: UpdatePasswordPayload): Observable<OperationStatusResponseDTO> {
    return this.http.put<OperationStatusResponseDTO>(apiRoutes.profile.password, payload);
  }

  updatePhoto(payload: UpdatePhotoPayload): Observable<ProfileDTO> {
    return this.http.put<ProfileDTO>(apiRoutes.profile.photo, payload);
  }
}
