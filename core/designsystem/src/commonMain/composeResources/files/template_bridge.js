(() => {
  'use strict';
  const bootstrap = __APP_BOOTSTRAP__;
  let active = null;
  let queue = Promise.resolve();
  function send(identity, event, data = {}) {
    const message = JSON.stringify({...bootstrap, ...identity, event, data});
    if (window.AppWebBridge) window.AppWebBridge.postMessage(message);
    else window.webkit?.messageHandlers?.appWeb?.postMessage(message);
  }
  window.AppContentHost = {
    dispatch(encoded) {
      const bytes = Uint8Array.from(atob(encoded), char => char.charCodeAt(0));
      const message = JSON.parse(new TextDecoder().decode(bytes));
      if (message.instance !== bootstrap.instance || message.protocol !== bootstrap.protocol ||
          message.templateId !== bootstrap.templateId || message.revision !== bootstrap.revision) return;
      let context = active;
      if (message.command === 'render') {
        active?.controller.abort();
        const controller = new AbortController();
        context = {
          controller,
          signal: controller.signal,
          isCurrent: () => active === context && !controller.signal.aborted,
          emit: (event, data) => { if (context.isCurrent()) send(message, event, data); }
        };
        active = context;
      }
      const bound = context;
      queue = queue.then(async () => {
        try {
          const renderer = window.AppContentTemplate;
          switch (message.command) {
            case 'render':
              if (!bound.isCurrent()) return;
              await renderer.render(message.payload, bound);
              if (bound.isCurrent()) send(message, 'rendered');
              break;
            case 'updateTheme':
              if (bound?.isCurrent()) await renderer.updateTheme(message.payload);
              break;
            case 'captureState':
              send(message, 'state', await renderer.captureState());
              break;
            case 'reset':
              active?.controller.abort(); active = null;
              await renderer.reset();
              send(message, 'reset');
              break;
          }
        } catch (_) {
          if (message.command !== 'render' || bound.isCurrent()) send(message, 'error');
        }
      });
    }
  };
  document.addEventListener('DOMContentLoaded', () => {
    const renderer = window.AppContentTemplate;
    if (!renderer || !['render', 'updateTheme', 'captureState', 'reset'].every(key => typeof renderer[key] === 'function')) {
      send({}, 'protocolError'); return;
    }
    send({}, 'ready');
  }, {once: true});
})();
