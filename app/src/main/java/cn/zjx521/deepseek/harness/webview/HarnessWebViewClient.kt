package cn.zjx521.deepseek.harness.webview

import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Custom WebViewClient for DeepSeek Harness.
 *
 * Responsibilities:
 * - Report page load lifecycle to the hosting Activity
 * - Detect and surface main-frame load errors (server not running, etc.)
 * - Proceed through SSL errors for LAN self-signed certificates
 * - Inject mobile-friendly CSS/JS tweaks after each page load
 * - Keep all navigation within the same WebView (no external browser launch)
 */
class HarnessWebViewClient(
    private val onPageStarted: () -> Unit,
    private val onPageFinished: () -> Unit,
    private val onError: (code: Int, description: String) -> Unit,
) : WebViewClient() {

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onPageStarted()
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        onPageFinished()
        injectMobileAdaptations(view)
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError,
    ) {
        super.onReceivedError(view, request, error)
        // Only surface errors for the top-level page; sub-resource errors are tolerated.
        if (request.isForMainFrame) {
            onError(error.errorCode, error.description?.toString() ?: "Unknown error")
        }
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        // Allow self-signed certs common on LAN development servers.
        // For a production remote setup, validate the cert properly instead.
        handler.proceed()
    }

    override fun onPageCommitVisible(view: WebView, url: String?) {
        super.onPageCommitVisible(view, url)
        injectMobileAdaptations(view)
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
        super.doUpdateVisitedHistory(view, url, isReload)
        injectMobileAdaptations(view)
    }

    /**
     * Injects a mobile adaptation layer into the desktop Harness Web UI.
     *
     * The Web UI is a desktop-first React app whose plugin CSS modules compile
     * to "hash_local" class names (e.g. pI_x6G_sidebarCol), so rules here use
     * suffix selectors ([class$="_local"]) plus semantic hooks (role="dialog",
     * <nav>, data-* attributes) instead of fragile substring guesses.
     *
     * Covered:
     *   • Sidebar: on narrow screens the expanded sidebar becomes an overlay
     *     drawer (with backdrop) instead of squeezing the conversation column.
     *   • Settings dialog: converted from a fixed two-column panel into a
     *     full-screen vertical layout with a top horizontal tab bar.
     *   • Text wrapping, edge-to-edge overlays and safe-area padding.
     */
    private fun injectMobileAdaptations(view: WebView) {
        val css = """
            /* ═══════════════════════════════════════════════════════════════
               DeepSeek Harness — Android mobile adaptation layer.
               Every layout override is gated behind the `dsh-mobile` class that the
               injected script adds to <html> on narrow viewports, so desktop / wide
               WebViews are never affected. Selectors use CSS-module suffix matching
               ([class$="_local"]) because plugin bundles compile to "hash_local".
               ═══════════════════════════════════════════════════════════════ */

            /* ── Global resets ── */
            html.dsh-mobile * {
              -webkit-tap-highlight-color: transparent;
            }
            html.dsh-mobile,
            html.dsh-mobile body {
              overflow-x: hidden !important;
              max-width: 100vw !important;
            }
            html.dsh-mobile body {
              padding-bottom: env(safe-area-inset-bottom, 0) !important;
              -webkit-font-smoothing: antialiased;
            }

            /* Prevent vertical single-character wrapping (leave code blocks alone) */
            html.dsh-mobile p,
            html.dsh-mobile h1, html.dsh-mobile h2, html.dsh-mobile h3,
            html.dsh-mobile h4, html.dsh-mobile h5, html.dsh-mobile h6,
            html.dsh-mobile span, html.dsh-mobile label, html.dsh-mobile div {
              word-break: normal !important;
              overflow-wrap: break-word !important;
            }

            /* Full-viewport overlays stay edge-to-edge */
            html.dsh-mobile [class$="_overlay"] {
              padding: 0 !important;
              inset: 0 !important;
            }

            /* ── Settings dialog → mobile full-screen layout ── */
            html.dsh-mobile .dsh-settings-dialog {
              width: 100vw !important;
              max-width: 100vw !important;
              height: 100vh !important;
              max-height: 100vh !important;
              border-radius: 0 !important;
              margin: 0 !important;
              display: flex !important;
              flex-direction: column !important;
              box-sizing: border-box !important;
            }

            /* Left nav rail becomes a top horizontal scrolling tab bar */
            html.dsh-mobile .dsh-settings-dialog > nav {
              flex: 0 0 auto !important;
              display: flex !important;
              flex-direction: row !important;
              align-items: center !important;
              width: 100% !important;
              min-width: 100% !important;
              padding: 8px 12px 6px !important;
              gap: 8px !important;
              overflow-x: auto !important;
              overflow-y: hidden !important;
              white-space: nowrap !important;
              border-bottom: 1px solid var(--dsw-alias-border-l1, rgba(255, 255, 255, 0.08)) !important;
              box-sizing: border-box !important;
              scrollbar-width: none !important;
            }
            html.dsh-mobile .dsh-settings-dialog > nav::-webkit-scrollbar {
              display: none !important;
            }

            html.dsh-mobile .dsh-settings-dialog > nav [class$="_navTitle"] {
              display: none !important;
            }

            html.dsh-mobile .dsh-settings-dialog > nav [class$="_navList"] {
              display: flex !important;
              flex-direction: row !important;
              gap: 6px !important;
              width: auto !important;
              overflow-x: auto !important;
              flex-wrap: nowrap !important;
              scrollbar-width: none !important;
            }
            html.dsh-mobile .dsh-settings-dialog > nav [class$="_navList"]::-webkit-scrollbar {
              display: none !important;
            }

            html.dsh-mobile .dsh-settings-dialog > nav button[class$="_navCell"] {
              flex: 0 0 auto !important;
              height: 36px !important;
              min-height: 36px !important;
              padding: 6px 14px !important;
              font-size: 13px !important;
              border-radius: 18px !important;
              white-space: nowrap !important;
              display: inline-flex !important;
              align-items: center !important;
              gap: 6px !important;
            }

            html.dsh-mobile .dsh-settings-dialog > [class$="_content"] {
              flex: 1 1 auto !important;
              width: 100% !important;
              min-width: 0 !important;
              min-height: 0 !important;
            }

            html.dsh-mobile .dsh-settings-dialog [class$="_header"] {
              height: auto !important;
              min-height: 48px !important;
              padding: 8px 12px !important;
              box-sizing: border-box !important;
            }

            html.dsh-mobile .dsh-settings-dialog [class$="_options"] {
              flex: 1 1 auto !important;
              width: 100% !important;
              padding: 10px 16px 36px !important;
              box-sizing: border-box !important;
              overflow-y: auto !important;
              overflow-x: hidden !important;
            }

            /* Form rows inside settings stay readable on narrow screens */
            html.dsh-mobile .dsh-settings-dialog [class$="_rowHead"] {
              display: flex !important;
              flex-wrap: wrap !important;
              align-items: center !important;
              justify-content: space-between !important;
              gap: 8px !important;
            }
            html.dsh-mobile .dsh-settings-dialog [class$="_rowActions"] {
              margin-left: auto !important;
              flex-shrink: 0 !important;
            }
            html.dsh-mobile .dsh-settings-dialog button,
            html.dsh-mobile .dsh-settings-dialog [role="button"] {
              white-space: nowrap !important;
              box-sizing: border-box !important;
            }

            /* ── Sidebar → overlay drawer on mobile ── */
            #dsh-sidebar-backdrop {
              display: none;
              position: fixed;
              inset: 0;
              z-index: 55;
              background: rgba(0, 0, 0, 0.45);
            }
            html.dsh-mobile.dsh-sidebar-open #dsh-sidebar-backdrop {
              display: block;
            }

            /* Expanded sidebar overlays the conversation instead of squeezing it */
            html.dsh-mobile.dsh-sidebar-open .dsh-frame {
              grid-template-columns: 0px minmax(0, 1fr) 0px !important;
            }
            html.dsh-mobile.dsh-sidebar-open .dsh-sidebar {
              position: fixed;
              left: 0;
              top: 0;
              bottom: 0;
              width: min(86vw, 340px);
              z-index: 60;
              box-shadow: 0 0 40px rgba(0, 0, 0, 0.55);
            }
            html.dsh-mobile.dsh-sidebar-open .dsh-frame > [class$="_centerCol"] {
              grid-column: 2;
              grid-row: 1;
            }
            html.dsh-mobile.dsh-sidebar-open .dsh-frame > [class$="_detailsCol"] {
              grid-column: 3;
              grid-row: 1;
            }

            /* ── Composer toolbar: compact model & permission chips ── */
            /* Model chip ("DeepSeek-V4-Pro") is narrower on mobile; the effort
               tag ("High") is dropped from the tiny trigger (the full value
               lives in the dropdown). */
            html.dsh-mobile [data-composer-card] [class$="_trigger"][aria-haspopup="menu"] {
              max-width: 150px !important;
              font-size: 12px !important;
            }
            html.dsh-mobile [data-composer-card] [class$="_trigger"][aria-haspopup="menu"] [class$="_triggerEffort"] {
              display: none !important;
            }
            /* Permission chip ("Workspace Write" / "Read-only"): icon + chevron only */
            html.dsh-mobile [data-composer-card] [class$="_triggerIcon"] + [class$="_triggerLabel"] {
              display: none !important;
            }
            /* Tighten the toolbar gaps so both sides fit on one line */
            html.dsh-mobile [data-composer-card] [class$="_tools"] {
              gap: 10px !important;
            }
            html.dsh-mobile [data-composer-card] [class$="_trailing"] {
              gap: 8px !important;
            }

            /* ── Hero workspace row (above the composer) ── */
            /* Cap the workspace name so the agent-preset chip ("Standard" /
               "标准") stays beside it instead of being pushed to the far right. */
            html.dsh-mobile [class$="_heroWorkspaceRow"] {
              padding-left: 8px !important;
            }
            html.dsh-mobile [class$="_heroWorkspaceRow"] [class$="_workspace"] {
              max-width: min(58%, 220px) !important;
              min-width: 0 !important;
            }
        """.trimIndent()

        val cssEncoded = android.util.Base64.encodeToString(
            css.toByteArray(Charsets.UTF_8),
            android.util.Base64.NO_WRAP
        )

        val js = """
            (function() {
                if (window.__dshMobileInstalled) return;
                window.__dshMobileInstalled = true;
                var css = atob('$cssEncoded');
                var style = document.getElementById('dsh-mobile-overrides');
                if (!style) {
                    style = document.createElement('style');
                    style.id = 'dsh-mobile-overrides';
                    (document.head || document.documentElement).appendChild(style);
                }
                style.textContent = css;
            var MOBILE_MAX = 900;
            var html = document.documentElement;

            function updateMobile() {
                var mobile = window.innerWidth <= MOBILE_MAX;
                html.classList.toggle('dsh-mobile', mobile);
                if (!mobile) html.classList.remove('dsh-sidebar-open');
            }
            updateMobile();
            var resizeTimer = null;
            window.addEventListener('resize', function() {
                if (resizeTimer) clearTimeout(resizeTimer);
                resizeTimer = setTimeout(updateMobile, 150);
            });

            var frame = null;
            var sidebar = null;
            var backdrop = null;

            function findFrame() {
                var candidates = document.querySelectorAll('[class$="_frame"]');
                for (var i = 0; i < candidates.length; i++) {
                    if (candidates[i].querySelector('[class$="_sidebarCol"]')) return candidates[i];
                }
                return null;
            }

            function initSidebarDrawer() {
                if (frame || !html.classList.contains('dsh-mobile')) return;
                var found = findFrame();
                if (!found) return;
                frame = found;
                frame.classList.add('dsh-frame');
                sidebar = frame.querySelector('[class$="_sidebarCol"]');
                if (sidebar) sidebar.classList.add('dsh-sidebar');

                backdrop = document.createElement('div');
                backdrop.id = 'dsh-sidebar-backdrop';
                document.body.appendChild(backdrop);

                var sync = function() {
                    var open = !frame.hasAttribute('data-sidebar-collapsed');
                    html.classList.toggle('dsh-sidebar-open', open);
                };
                sync();
                new MutationObserver(sync).observe(frame, {
                    attributes: true,
                    attributeFilter: ['data-sidebar-collapsed']
                });

                backdrop.addEventListener('click', function() {
                    var toggle = sidebar ? sidebar.querySelector('button[class$="_toggle"]') : null;
                    if (toggle) toggle.click();
                });

                if (sidebar) {
                    sidebar.addEventListener('click', function(e) {
                        if (!html.classList.contains('dsh-sidebar-open')) return;
                        var row = e.target.closest('[class$="_sessionRow"], [class$="_searchResultRow"]');
                        if (!row) return;
                        setTimeout(function() {
                            if (!html.classList.contains('dsh-sidebar-open')) return;
                            var toggle = sidebar.querySelector('button[class$="_toggle"]');
                            if (toggle) toggle.click();
                        }, 0);
                    }, true);
                }
            }

            function markSettingsDialogs() {
                var dialogs = document.querySelectorAll('[role="dialog"]');
                for (var i = 0; i < dialogs.length; i++) {
                    var d = dialogs[i];
                    var hasNav = false;
                    for (var j = 0; j < d.children.length; j++) {
                        if (d.children[j].tagName === 'NAV') { hasNav = true; break; }
                    }
                    d.classList.toggle('dsh-settings-dialog', hasNav);
                }
            }

            var lastModalState = false;
            function checkModal() {
                var hasModal = !!document.querySelector('[role="dialog"], [role="menu"], [aria-modal="true"]')
                    || html.classList.contains('dsh-sidebar-open');
                if (hasModal !== lastModalState) {
                    lastModalState = hasModal;
                    if (window.AndroidBridge && window.AndroidBridge.reportModalState) {
                        window.AndroidBridge.reportModalState(hasModal);
                    }
                }
            }

            function run() {
                initSidebarDrawer();
                markSettingsDialogs();
                checkModal();
            }
            run();

            var pending = false;
            var observer = new MutationObserver(function() {
                if (pending) return;
                pending = true;
                requestAnimationFrame(function() {
                    pending = false;
                    run();
                });
            });
            observer.observe(document.body || document.documentElement, { childList: true, subtree: true });

            var tries = 0;
            var retry = setInterval(function() {
                tries++;
                run();
                if (tries > 40 || frame) clearInterval(retry);
            }, 250);
            })();
        """.trimIndent()

        view.evaluateJavascript(js, null)
    }
}
