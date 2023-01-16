export type CreateDTO<T> = Omit<T, "id">;

export type ErrorCallback = (error: string) => void;

async function getErrorCallbackAsync(response: Response) {
  return `${response.status} - ${
    response.statusText
  } => ${await response.text()}`;
}

export async function callErrorCallbackOnErrorAsync(
  response: Response,
  onErrorCallback?: ErrorCallback
) {
  if (onErrorCallback !== undefined) {
    onErrorCallback(await getErrorCallbackAsync(response));
  }
}
