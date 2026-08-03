export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (url.pathname === "/search") {
      const targetUrl = `https://service.relzarick.com/search${url.search}`;

      const newHeaders = new Headers(request.headers);
      newHeaders.delete("Host");

      const proxyRequest = new Request(targetUrl, {
        method: request.method,
        headers: newHeaders,
        body:
          request.method !== "GET" && request.method !== "HEAD"
            ? request.body
            : null,
      });

      return fetch(proxyRequest);
    }

    return env.ASSETS.fetch(request);
  },
};
