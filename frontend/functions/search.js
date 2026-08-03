export async function onRequest(context) {
  const originalRequest = context.request;
  const originalUrl = new URL(originalRequest.url);

  const targetUrl = `https://service.relzarick.com/search${originalUrl.search}`;

  const proxyRequest = new Request(targetUrl, {
    method: originalRequest.method,
    headers: originalRequest.headers,
  });

  return fetch(proxyRequest);
}
