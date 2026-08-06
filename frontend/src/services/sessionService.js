import { appConfig } from '../config/env';

const unwrapJson = async (response) => {
  const text = await response.text();
  if (!text) {
    return null;
  }

  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
};

export async function prepareLogoutSession({ accessToken, idToken, refreshToken }) {
  const response = await fetch(`${appConfig.apiUrl}/authz/logout`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
    },
    body: JSON.stringify({
      idToken,
      refreshToken,
    }),
  });

  const payload = await unwrapJson(response);

  if (!response.ok) {
    const message = payload?.message
      || payload?.error
      || `No fue posible preparar el logout (${response.status})`;
    throw new Error(message);
  }

  return payload;
}
