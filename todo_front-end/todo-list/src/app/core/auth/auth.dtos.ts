export type AuthenticationDTO = {
  readonly login: string;
  readonly password: string;
};

export type LoginResponseDTO = {
  readonly token: string;
};
