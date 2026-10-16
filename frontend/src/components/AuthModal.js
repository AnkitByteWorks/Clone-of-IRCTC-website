import { store } from '../state/store.js';
import { AuthApi } from '../api/client.js';

export function renderAuthModal(container) {
  const state = store.getState();
  if (state.activeModal !== 'auth') {
    container.innerHTML = '';
    return;
  }

  const isLogin = state.authTab === 'login';

  container.innerHTML = `
    <div id="auth-modal-backdrop" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-fade-in">
      <div id="auth-modal-card" class="w-full max-w-md rounded-2xl glass-panel-elevated p-6 relative overflow-hidden shadow-2xl border border-slate-800 bg-slate-900/95">
        
        <!-- Close button -->
        <button id="close-auth-btn" class="absolute top-4 right-4 text-slate-400 hover:text-white p-1.5 rounded-lg hover:bg-slate-800/80 transition">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>
        </button>

        <!-- Brand Icon & Header -->
        <div class="text-center mb-6">
          <div class="inline-flex w-12 h-12 rounded-2xl bg-gradient-to-tr from-cyan-500 to-blue-600 items-center justify-center text-slate-950 mb-3 shadow-lg shadow-cyan-500/20">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-6 h-6 stroke-slate-950 fill-none stroke-[2.2]" viewBox="0 0 24 24">
              <rect width="16" height="16" x="4" y="3" rx="2"/>
              <path d="M4 11h16"/><path d="M12 3v8"/><path d="m8 19-2 3"/><path d="m18 22-2-3"/><circle cx="8" cy="15" r="1"/><circle cx="16" cy="15" r="1"/>
            </svg>
          </div>
          <h2 class="text-xl font-bold text-white tracking-tight">${isLogin ? 'Sign In to RailConnect' : 'Create RailConnect Account'}</h2>
          <p class="text-xs text-slate-400 mt-1">Next-Gen IRCTC Certified High-Speed Ticketing</p>
        </div>

        <!-- Auth Tabs Switcher -->
        <div class="flex rounded-xl bg-slate-950 p-1 border border-slate-800 mb-6">
          <button id="switch-to-login" class="flex-1 py-2 rounded-lg text-xs font-semibold transition-all ${isLogin ? 'bg-cyan-500 text-slate-950 font-bold shadow' : 'text-slate-400 hover:text-white'}">
            Sign In
          </button>
          <button id="switch-to-register" class="flex-1 py-2 rounded-lg text-xs font-semibold transition-all ${!isLogin ? 'bg-cyan-500 text-slate-950 font-bold shadow' : 'text-slate-400 hover:text-white'}">
            Register
          </button>
        </div>

        <!-- Error Alert -->
        <div id="auth-error-box" class="hidden mb-4 p-3 rounded-xl bg-red-500/10 border border-red-500/30 text-red-400 text-xs flex items-center gap-2">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
          <span id="auth-error-msg"></span>
        </div>

        <!-- Form -->
        <form id="auth-form" class="space-y-4">
          ${!isLogin ? `
            <div>
              <label class="block text-xs font-medium text-slate-300 mb-1.5">Full Name (as per Govt ID)</label>
              <div class="relative">
                <input 
                  type="text" 
                  id="auth-name" 
                  name="fullName" 
                  required 
                  placeholder="e.g. Ramesh Kumar"
                  class="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500 transition"
                />
              </div>
            </div>

            <div>
              <label class="block text-xs font-medium text-slate-300 mb-1.5">Mobile Number (Indian 10-Digit)</label>
              <div class="relative flex">
                <span class="inline-flex items-center px-3 rounded-l-xl bg-slate-800/80 border border-r-0 border-slate-800 text-slate-400 text-xs font-mono font-medium">+91</span>
                <input 
                  type="tel" 
                  id="auth-phone" 
                  name="phoneNumber" 
                  required 
                  pattern="^[6-9]\\d{9}$"
                  maxlength="10"
                  placeholder="9876543210"
                  class="w-full px-3.5 py-2.5 rounded-r-xl bg-slate-950 border border-slate-800 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500 font-mono transition"
                />
              </div>
            </div>
          ` : ''}

          <div>
            <label class="block text-xs font-medium text-slate-300 mb-1.5">Email Address</label>
            <input 
              type="email" 
              id="auth-email" 
              name="email" 
              required 
              placeholder="passenger@railconnect.in"
              class="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500 transition"
            />
          </div>

          <div>
            <label class="block text-xs font-medium text-slate-300 mb-1.5">Password</label>
            <input 
              type="password" 
              id="auth-password" 
              name="password" 
              required 
              minlength="8"
              placeholder="••••••••"
              class="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500 transition font-mono"
            />
            ${!isLogin ? '<p class="text-[10px] text-slate-500 mt-1">Must be at least 8 characters</p>' : ''}
          </div>

          <button 
            type="submit" 
            id="auth-submit-btn" 
            class="w-full py-3 rounded-xl font-bold text-sm bg-gradient-to-r from-cyan-500 to-blue-600 text-slate-950 hover:from-cyan-400 hover:to-blue-500 shadow-lg shadow-cyan-500/25 transition-all flex items-center justify-center gap-2 mt-2"
          >
            <span id="auth-btn-text">${isLogin ? 'Sign In & Access Bookings' : 'Register RailConnect Account'}</span>
            <svg id="auth-spinner" class="hidden animate-spin h-4 w-4 text-slate-950" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
          </button>
        </form>

        <div class="mt-6 pt-4 border-t border-slate-800/80 text-center text-xs text-slate-500">
          <span>Protected by RailConnect 256-bit RFC 7519 JWT Encryption</span>
        </div>

      </div>
    </div>
  `;

  // Attach event listeners
  const backdrop = container.querySelector('#auth-modal-backdrop');
  const card = container.querySelector('#auth-modal-card');
  const closeBtn = container.querySelector('#close-auth-btn');
  const switchToLogin = container.querySelector('#switch-to-login');
  const switchToRegister = container.querySelector('#switch-to-register');
  const form = container.querySelector('#auth-form');
  const errorBox = container.querySelector('#auth-error-box');
  const errorMsg = container.querySelector('#auth-error-msg');
  const submitBtn = container.querySelector('#auth-submit-btn');
  const btnText = container.querySelector('#auth-btn-text');
  const spinner = container.querySelector('#auth-spinner');

  closeBtn?.addEventListener('click', () => store.closeModal());
  backdrop?.addEventListener('click', (e) => {
    if (e.target === backdrop) store.closeModal();
  });

  switchToLogin?.addEventListener('click', () => {
    store.setAuthTab('login');
  });

  switchToRegister?.addEventListener('click', () => {
    store.setAuthTab('register');
  });

  form?.addEventListener('submit', async (e) => {
    e.preventDefault();
    errorBox.classList.add('hidden');
    submitBtn.disabled = true;
    btnText.textContent = isLogin ? 'Signing In...' : 'Registering...';
    spinner.classList.remove('hidden');

    try {
      const email = form.email.value.trim();
      const password = form.password.value;

      let authRes;
      if (isLogin) {
        authRes = await AuthApi.login({ email, password });
      } else {
        const fullName = form.fullName.value.trim();
        const phoneNumber = form.phoneNumber.value.trim();
        authRes = await AuthApi.register({ fullName, email, phoneNumber, password });
      }

      // Store in state
      const user = {
        userId: authRes.userId,
        fullName: authRes.fullName,
        email: authRes.email,
        role: authRes.role,
      };
      store.setAuth(user, authRes.accessToken);
    } catch (err) {
      errorMsg.textContent = err.message || 'Authentication failed. Please verify credentials.';
      errorBox.classList.remove('hidden');
    } finally {
      submitBtn.disabled = false;
      btnText.textContent = isLogin ? 'Sign In & Access Bookings' : 'Register RailConnect Account';
      spinner.classList.add('hidden');
    }
  });
}
