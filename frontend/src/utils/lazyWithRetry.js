import { lazy } from 'react';

/**
 * Wraps dynamic import() with an automatic retry on network/chunk load failures.
 * This prevents users from getting stuck when a new build is deployed to production.
 */
export function lazyWithRetry(componentImport) {
  return lazy(async () => {
    const pageHasBeenForceRefreshed = JSON.parse(
      window.sessionStorage.getItem('pfm_chunk_reload') || 'false'
    );

    try {
      const component = await componentImport();
      window.sessionStorage.setItem('pfm_chunk_reload', 'false');
      return component;
    } catch (error) {
      if (!pageHasBeenForceRefreshed) {
        window.sessionStorage.setItem('pfm_chunk_reload', 'true');
        window.location.reload();
        return { default: () => null };
      }
      throw error;
    }
  });
}
