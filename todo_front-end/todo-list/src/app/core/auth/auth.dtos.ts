export type AuthenticationDTO = {
  readonly login: string;
  readonly password: string;
};

export type ProfileDTO = {
  readonly id: number;
  readonly email: string;
  readonly displayName: string;
  readonly photoDataUrl: string | null;
};

export type LoginResponseDTO = {
  readonly token: string;
  readonly refreshToken: string;
  readonly profile: ProfileDTO;
};

export type TokenRefreshRequestDTO = {
  readonly refreshToken: string;
};

export type TokenRefreshResponseDTO = {
  readonly token: string;
  readonly refreshToken: string;
};

export type PasswordResetConfirmDTO = {
  readonly token: string;
  readonly newPassword: string;
  readonly confirmPassword: string;
};

export type OperationStatusResponseDTO = {
  readonly status: string;
  readonly message: string;
};
