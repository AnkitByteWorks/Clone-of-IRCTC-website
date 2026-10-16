import { store } from '../state/store.js';

export function renderNavbar(container) {
  const state = store.getState();
  const { currentUser, infrastructureStatus, activeTab, notifications } = state;

  const isAllUp = 
    infrastructureStatus.mysql === 'UP' &&
    infrastructureStatus.mongodb === 'UP' &&
    infrastructureStatus.redis === 'UP' &&
    infrastructureStatus.kafka === 'UP';

  const unreadCount = notifications.filter(n => !n.readStatus).length;

  container.innerHTML = `
    <header class="sticky top-0 z-40 w-full border-b border-slate-800 bg-slate-950/85 backdrop-blur-md">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        
        <!-- Logo & Infrastructure Badge -->
        <div class="flex items-center gap-4">
          <button id="nav-brand-btn" class="flex items-center gap-2.5 text-left group">
            <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-cyan-500 to-blue-600 flex items-center justify-center text-slate-950 shadow-lg shadow-cyan-500/20 group-hover:scale-105 transition-transform">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-6 h-6 stroke-slate-950 fill-none stroke-[2.2]" viewBox="0 0 24 24">
                <rect width="16" height="16" x="4" y="3" rx="2"/>
                <path d="M4 11h16"/><path d="M12 3v8"/><path d="m8 19-2 3"/><path d="m18 22-2-3"/><circle cx="8" cy="15" r="1"/><circle cx="16" cy="15" r="1"/>
              </svg>
            </div>
            <div>
              <div class="flex items-center gap-1.5">
                <span class="font-extrabold tracking-tight text-lg text-white font-mono">Rail<span class="text-cyan-400">Connect</span></span>
                <span class="px-1.5 py-0.2 rounded text-[10px] font-bold bg-cyan-950/80 text-cyan-400 border border-cyan-800/60 uppercase">2026</span>
              </div>
              <p class="text-[10px] text-slate-400 -mt-0.5 tracking-wider uppercase font-medium">IRCTC Next-Gen Core</p>
            </div>
          </button>

          <!-- Live Infrastructure Status Badge -->
          <div class="hidden xl:flex items-center gap-2 px-3 py-1 rounded-full bg-slate-900 border border-slate-800 text-xs">
            <span class="flex h-2 w-2 relative">
              <span class="animate-ping absolute inline-flex h-full w-full rounded-full ${isAllUp ? 'bg-emerald-400' : 'bg-amber-400'} opacity-75"></span>
              <span class="relative inline-flex rounded-full h-2 w-2 ${isAllUp ? 'bg-emerald-500' : 'bg-amber-500'}"></span>
            </span>
            <span class="text-slate-400 text-[11px] font-mono">
              MySQL <span class="${infrastructureStatus.mysql === 'UP' ? 'text-emerald-400' : 'text-amber-400'} font-bold">●</span>
              Mongo <span class="${infrastructureStatus.mongodb === 'UP' ? 'text-emerald-400' : 'text-amber-400'} font-bold">●</span>
              Redis <span class="${infrastructureStatus.redis === 'UP' ? 'text-emerald-400' : 'text-amber-400'} font-bold">●</span>
              Kafka <span class="${infrastructureStatus.kafka === 'UP' ? 'text-emerald-400' : 'text-amber-400'} font-bold">●</span>
            </span>
          </div>
        </div>

        <!-- Navigation Tabs -->
        <nav class="hidden md:flex items-center gap-1 bg-slate-900/80 p-1 rounded-xl border border-slate-800">
          <button data-tab="search" class="nav-tab-btn px-3.5 py-1.5 rounded-lg text-xs font-semibold transition-all flex items-center gap-1.5 ${activeTab === 'search' ? 'bg-cyan-500 text-slate-950 shadow-md font-bold' : 'text-slate-300 hover:text-white hover:bg-slate-800/60'}">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
            Search Trains
          </button>
          <button data-tab="pnr" class="nav-tab-btn px-3.5 py-1.5 rounded-lg text-xs font-semibold transition-all flex items-center gap-1.5 ${activeTab === 'pnr' ? 'bg-cyan-500 text-slate-950 shadow-md font-bold' : 'text-slate-300 hover:text-white hover:bg-slate-800/60'}">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
            PNR Status
          </button>
          <button data-tab="bookings" class="nav-tab-btn px-3.5 py-1.5 rounded-lg text-xs font-semibold transition-all flex items-center gap-1.5 ${activeTab === 'bookings' ? 'bg-cyan-500 text-slate-950 shadow-md font-bold' : 'text-slate-300 hover:text-white hover:bg-slate-800/60'}">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect width="18" height="18" x="3" y="4" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
            My Bookings
          </button>
          <button data-tab="masterList" class="nav-tab-btn px-3.5 py-1.5 rounded-lg text-xs font-semibold transition-all flex items-center gap-1.5 ${activeTab === 'masterList' ? 'bg-cyan-500 text-slate-950 shadow-md font-bold' : 'text-slate-300 hover:text-white hover:bg-slate-800/60'}">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
            Master Passenger List
          </button>
        </nav>

        <!-- Right Side: Auth / User Profile & Notifications -->
        <div class="flex items-center gap-3">
          ${currentUser ? `
            <div class="flex items-center gap-2.5 sm:gap-3">
              <!-- Notification Bell Button -->
              <button id="notification-bell-btn" class="relative p-2 rounded-xl text-slate-300 hover:text-white hover:bg-slate-900 border border-slate-800 transition" title="Kafka Notifications">
                <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/>
                </svg>
                ${unreadCount > 0 ? `
                  <span class="absolute -top-1 -right-1 flex h-4 min-w-4 px-1 items-center justify-center rounded-full bg-cyan-500 text-[10px] font-bold text-slate-950 font-mono shadow-sm animate-pulse">
                    ${unreadCount > 9 ? '9+' : unreadCount}
                  </span>
                ` : ''}
              </button>

              <div class="hidden sm:flex flex-col text-right">
                <span class="text-xs font-semibold text-white">${currentUser.fullName}</span>
                <span class="text-[10px] text-cyan-400 font-mono">${currentUser.email}</span>
              </div>
              <div class="w-9 h-9 rounded-full bg-cyan-500/10 border border-cyan-500/30 text-cyan-400 font-bold text-xs flex items-center justify-center">
                ${currentUser.fullName ? currentUser.fullName.slice(0, 2).toUpperCase() : 'RC'}
              </div>
              <button id="logout-btn" title="Sign Out" class="p-2 rounded-lg text-slate-400 hover:text-red-400 hover:bg-slate-900 border border-transparent hover:border-slate-800 transition">
                <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
              </button>
            </div>
          ` : `
            <button id="open-auth-btn" class="px-4 py-2 rounded-xl text-xs font-bold bg-cyan-500 text-slate-950 hover:bg-cyan-400 shadow-md shadow-cyan-500/20 hover:shadow-cyan-500/30 transition-all flex items-center gap-1.5">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              Login / Register
            </button>
          `}
        </div>

      </div>
    </header>
  `;

  // Attach event handlers
  container.querySelector('#nav-brand-btn')?.addEventListener('click', () => {
    store.setActiveTab('search');
  });

  container.querySelector('#notification-bell-btn')?.addEventListener('click', () => {
    store.toggleNotificationDrawer(true);
  });

  container.querySelectorAll('.nav-tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const tab = btn.getAttribute('data-tab');
      if ((tab === 'bookings' || tab === 'masterList') && !store.getState().currentUser) {
        store.setAuthTab('login');
        store.openModal('auth');
        return;
      }
      store.setActiveTab(tab);
    });
  });

  container.querySelector('#open-auth-btn')?.addEventListener('click', () => {
    store.setAuthTab('login');
    store.openModal('auth');
  });

  container.querySelector('#logout-btn')?.addEventListener('click', () => {
    store.logout();
  });
}
