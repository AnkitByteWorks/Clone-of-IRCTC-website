import { store } from '../state/store.js';
import { CatalogApi, InventoryApi } from '../api/client.js';

export function renderHeroSearch(container) {
  const state = store.getState();
  const { searchParams, selectedQuota, isSearching } = state;

  const today = new Date().toISOString().split('T')[0];
  const tomorrow = new Date(Date.now() + 86400000).toISOString().split('T')[0];
  const dayAfter = new Date(Date.now() + 172800000).toISOString().split('T')[0];

  container.innerHTML = `
    <div class="relative overflow-hidden py-10 sm:py-16 bg-gradient-to-b from-slate-950 via-slate-900 to-slate-950 border-b border-slate-800">
      
      <!-- Subtle ambient glowing orbs -->
      <div class="absolute -top-32 left-1/4 w-96 h-96 bg-cyan-500/10 rounded-full blur-3xl pointer-events-none"></div>
      <div class="absolute top-1/2 right-1/4 w-80 h-80 bg-blue-600/10 rounded-full blur-3xl pointer-events-none"></div>

      <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        
        <!-- Hero Tagline -->
        <div class="text-center max-w-2xl mx-auto mb-8">
          <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-cyan-950/80 border border-cyan-800/60 text-cyan-400 text-xs font-semibold mb-3">
            <span class="w-2 h-2 rounded-full bg-cyan-400 animate-pulse"></span>
            Superfast IRCTC Certified Booking Engine
          </div>
          <h1 class="text-3xl sm:text-5xl font-extrabold text-white tracking-tight leading-tight">
            Book Train Tickets <span class="text-transparent bg-clip-text bg-gradient-to-r from-cyan-400 to-blue-500">at Lightning Speed</span>
          </h1>
          <p class="text-slate-400 text-sm sm:text-base mt-2">
            ACID-compliant live inventory, millisecond Tatkal booking lock, and zero-clutter modern experience.
          </p>
        </div>

        <!-- Main Search Card -->
        <div class="glass-panel-elevated rounded-2xl p-5 sm:p-7 shadow-2xl">
          
          <!-- Top Row: Quota Selector & Rules Info -->
          <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-5 mb-5 border-b border-slate-800">
            <div class="flex items-center gap-2">
              <span class="text-xs font-semibold text-slate-400 uppercase tracking-wider">Booking Quota:</span>
              <div class="flex rounded-xl bg-slate-950 p-1 border border-slate-800">
                <button 
                  id="quota-general-btn" 
                  class="px-4 py-1.5 rounded-lg text-xs font-semibold transition-all ${selectedQuota === 'GENERAL' ? 'bg-cyan-500 text-slate-950 font-bold shadow' : 'text-slate-400 hover:text-white'}"
                >
                  GENERAL (GN)
                </button>
                <button 
                  id="quota-tatkal-btn" 
                  class="px-4 py-1.5 rounded-lg text-xs font-semibold transition-all flex items-center gap-1.5 ${selectedQuota === 'TATKAL' ? 'bg-amber-400 text-slate-950 font-bold shadow' : 'text-amber-400/70 hover:text-amber-300'}"
                >
                  <span>TATKAL (TQ ⚡)</span>
                </button>
              </div>
            </div>

            <!-- Tatkal / Quota Rules Indicator -->
            ${selectedQuota === 'TATKAL' ? `
              <div class="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-300 text-xs">
                <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 text-amber-400 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
                <span><strong>Tatkal Active:</strong> 10:00 AM AC • 11:00 AM Non-AC • Max 4 passengers</span>
              </div>
            ` : `
              <div class="flex items-center gap-2 text-xs text-slate-500">
                <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 text-cyan-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
                <span>General Quota: Advance reservation up to 120 days • Max 6 passengers</span>
              </div>
            `}
          </div>

          <!-- Input Fields Row -->
          <div class="grid grid-cols-1 lg:grid-cols-12 gap-4 items-center">
            
            <!-- From Station (4 cols) -->
            <div class="lg:col-span-4 relative">
              <label class="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-1.5">From Station</label>
              <div class="relative">
                <div class="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-cyan-400">
                  <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M12 2a8 8 0 0 0-8 8c0 5.25 8 12 8 12s8-6.75 8-12a8 8 0 0 0-8-8z"/></svg>
                </div>
                <input 
                  type="text" 
                  id="from-station-input" 
                  value="${searchParams.from ? `${searchParams.from} - ${searchParams.fromName || ''}` : ''}"
                  placeholder="Enter station name or code (e.g. PNBE)"
                  autocomplete="off"
                  class="w-full pl-10 pr-4 py-3 rounded-xl bg-slate-950 border border-slate-800 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500 font-medium transition"
                />
              </div>
              <div id="from-station-dropdown" class="hidden absolute left-0 right-0 top-full mt-1.5 rounded-xl bg-slate-900 border border-slate-700 shadow-2xl z-50 max-h-64 overflow-y-auto"></div>
            </div>

            <!-- Swap Stations Button (1 col) -->
            <div class="lg:col-span-1 flex justify-center py-1 lg:py-0 lg:pt-5">
              <button 
                id="swap-stations-btn" 
                title="Swap Stations"
                class="w-10 h-10 rounded-xl bg-slate-950 hover:bg-slate-800 border border-slate-800 hover:border-cyan-500/50 text-slate-400 hover:text-cyan-400 flex items-center justify-center transition-all shadow-md group"
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 group-hover:rotate-180 transition-transform duration-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m16 3 4 4-4 4"/><path d="M20 7H4"/><path d="m8 21-4-4 4-4"/><path d="M4 17h16"/></svg>
              </button>
            </div>

            <!-- To Station (4 cols) -->
            <div class="lg:col-span-4 relative">
              <label class="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-1.5">To Station</label>
              <div class="relative">
                <div class="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-emerald-400">
                  <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/><circle cx="12" cy="10" r="3"/></svg>
                </div>
                <input 
                  type="text" 
                  id="to-station-input" 
                  value="${searchParams.to ? `${searchParams.to} - ${searchParams.toName || ''}` : ''}"
                  placeholder="Enter destination (e.g. NDLS)"
                  autocomplete="off"
                  class="w-full pl-10 pr-4 py-3 rounded-xl bg-slate-950 border border-slate-800 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500 font-medium transition"
                />
              </div>
              <div id="to-station-dropdown" class="hidden absolute left-0 right-0 top-full mt-1.5 rounded-xl bg-slate-900 border border-slate-700 shadow-2xl z-50 max-h-64 overflow-y-auto"></div>
            </div>

            <!-- Date Picker (3 cols) -->
            <div class="lg:col-span-3">
              <div class="flex items-center justify-between mb-1.5">
                <label class="text-xs font-semibold text-slate-400 uppercase tracking-wider">Date of Journey</label>
                <div class="flex items-center gap-1.5">
                  <button id="quick-tomorrow-btn" class="text-[10px] px-2 py-0.5 rounded bg-slate-950 hover:bg-slate-800 text-cyan-400 border border-slate-800 transition">Tomorrow</button>
                  <button id="quick-dayafter-btn" class="text-[10px] px-2 py-0.5 rounded bg-slate-950 hover:bg-slate-800 text-cyan-400 border border-slate-800 transition">+2 Days</button>
                </div>
              </div>
              <div class="relative">
                <input 
                  type="date" 
                  id="journey-date-input" 
                  min="${today}"
                  value="${searchParams.date || tomorrow}"
                  class="w-full px-4 py-3 rounded-xl bg-slate-950 border border-slate-800 text-sm text-white focus:outline-none focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500 font-mono transition"
                />
              </div>
            </div>

          </div>

          <!-- Bottom Row: Search Submit Button -->
          <div class="mt-6 flex flex-col sm:flex-row items-center justify-between gap-4 pt-4 border-t border-slate-800/80">
            <div class="flex flex-wrap items-center gap-2 text-xs text-slate-400">
              <span class="text-slate-500">Popular Routes:</span>
              <button data-quick="PNBE-NDLS" class="popular-route-btn px-2.5 py-1 rounded-lg bg-slate-950 hover:bg-slate-800 border border-slate-800 text-slate-300 hover:text-cyan-400 transition">
                Patna ⇄ New Delhi
              </button>
              <button data-quick="HWH-NDLS" class="popular-route-btn px-2.5 py-1 rounded-lg bg-slate-950 hover:bg-slate-800 border border-slate-800 text-slate-300 hover:text-cyan-400 transition">
                Howrah ⇄ New Delhi
              </button>
              <button data-quick="BCT-NDLS" class="popular-route-btn px-2.5 py-1 rounded-lg bg-slate-950 hover:bg-slate-800 border border-slate-800 text-slate-300 hover:text-cyan-400 transition">
                Mumbai ⇄ New Delhi
              </button>
            </div>

            <button 
              id="search-trains-btn" 
              class="w-full sm:w-auto px-8 py-3.5 rounded-xl font-bold text-sm bg-gradient-to-r from-cyan-500 to-blue-600 text-slate-950 hover:from-cyan-400 hover:to-blue-500 shadow-xl shadow-cyan-500/25 transition-all flex items-center justify-center gap-2"
              ${isSearching ? 'disabled' : ''}
            >
              ${isSearching ? `
                <svg class="animate-spin h-4 w-4 text-slate-950" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                <span>Searching Live Schedules...</span>
              ` : `
                <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
                <span>Search Live Trains</span>
              `}
            </button>
          </div>

        </div>

      </div>
    </div>
  `;

  // Autocomplete setup helper
  function setupAutocomplete(inputEl, dropdownEl, onSelect) {
    let debounceTimer;

    inputEl.addEventListener('input', (e) => {
      clearTimeout(debounceTimer);
      const query = e.target.value.trim();
      if (query.length < 2) {
        dropdownEl.classList.add('hidden');
        dropdownEl.innerHTML = '';
        return;
      }

      debounceTimer = setTimeout(async () => {
        try {
          const stations = await CatalogApi.searchStations(query);
          if (!stations || stations.length === 0) {
            dropdownEl.innerHTML = `<div class="p-3 text-xs text-slate-400 text-center">No matching stations found</div>`;
            dropdownEl.classList.remove('hidden');
            return;
          }

          dropdownEl.innerHTML = stations.map(s => `
            <div data-code="${s.stationCode}" data-name="${s.stationName}" class="station-item p-3 hover:bg-slate-800/80 cursor-pointer border-b border-slate-800/50 last:border-0 flex items-center justify-between transition">
              <div class="flex items-center gap-2.5">
                <span class="px-2 py-0.5 rounded bg-cyan-950 text-cyan-400 border border-cyan-800/50 font-mono text-xs font-bold">${s.stationCode}</span>
                <div>
                  <p class="text-xs font-semibold text-white">${s.stationName}</p>
                  <p class="text-[10px] text-slate-400">${s.city}, ${s.state}</p>
                </div>
              </div>
              <span class="text-[10px] font-mono text-slate-500">${s.zone || ''}</span>
            </div>
          `).join('');

          dropdownEl.classList.remove('hidden');

          dropdownEl.querySelectorAll('.station-item').forEach(item => {
            item.addEventListener('click', () => {
              const code = item.getAttribute('data-code');
              const name = item.getAttribute('data-name');
              inputEl.value = `${code} - ${name}`;
              dropdownEl.classList.add('hidden');
              onSelect(code, name);
            });
          });
        } catch (err) {
          console.warn('Station search failed:', err);
        }
      }, 250);
    });

    // Close dropdown when clicking outside
    document.addEventListener('click', (e) => {
      if (!inputEl.contains(e.target) && !dropdownEl.contains(e.target)) {
        dropdownEl.classList.add('hidden');
      }
    });
  }

  const fromInput = container.querySelector('#from-station-input');
  const fromDropdown = container.querySelector('#from-station-dropdown');
  const toInput = container.querySelector('#to-station-input');
  const toDropdown = container.querySelector('#to-station-dropdown');
  const swapBtn = container.querySelector('#swap-stations-btn');
  const dateInput = container.querySelector('#journey-date-input');
  const searchBtn = container.querySelector('#search-trains-btn');
  const quotaGeneralBtn = container.querySelector('#quota-general-btn');
  const quotaTatkalBtn = container.querySelector('#quota-tatkal-btn');
  const quickTomorrowBtn = container.querySelector('#quick-tomorrow-btn');
  const quickDayafterBtn = container.querySelector('#quick-dayafter-btn');

  setupAutocomplete(fromInput, fromDropdown, (code, name) => {
    store.setSearchParams({ from: code, fromName: name });
  });

  setupAutocomplete(toInput, toDropdown, (code, name) => {
    store.setSearchParams({ to: code, toName: name });
  });

  swapBtn?.addEventListener('click', () => {
    const cur = store.getState().searchParams;
    store.setSearchParams({
      from: cur.to,
      fromName: cur.toName,
      to: cur.from,
      toName: cur.fromName,
    });
  });

  quotaGeneralBtn?.addEventListener('click', () => {
    store.setQuota('GENERAL');
  });

  quotaTatkalBtn?.addEventListener('click', () => {
    store.setQuota('TATKAL');
  });

  quickTomorrowBtn?.addEventListener('click', () => {
    store.setSearchParams({ date: tomorrow });
  });

  quickDayafterBtn?.addEventListener('click', () => {
    store.setSearchParams({ date: dayAfter });
  });

  dateInput?.addEventListener('change', (e) => {
    store.setSearchParams({ date: e.target.value });
  });

  container.querySelectorAll('.popular-route-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const route = btn.getAttribute('data-quick');
      const [from, to] = route.split('-');
      const stationNames = {
        'PNBE': 'Patna Junction',
        'NDLS': 'New Delhi',
        'HWH': 'Howrah Junction',
        'BCT': 'Mumbai Central',
      };
      store.setSearchParams({
        from,
        fromName: stationNames[from] || from,
        to,
        toName: stationNames[to] || to,
      });
      triggerSearch();
    });
  });

  async function triggerSearch() {
    const s = store.getState().searchParams;
    if (!s.from || !s.to || !s.date) {
      alert('Please select From, To, and Journey Date');
      return;
    }

    store.setSearching(true);
    try {
      const results = await CatalogApi.searchTrains(s.from, s.to, s.date);
      store.setSearchResults(results);

      // Concurrently fetch live availability for each returned train
      results.forEach(train => {
        InventoryApi.getTrainAvailability(train.trainNumber, s.date)
          .then(avail => {
            store.setLiveAvailability(train.trainNumber, avail);
          })
          .catch(err => {
            console.warn(`Could not load live availability for train ${train.trainNumber}:`, err);
          });
      });
    } catch (err) {
      store.setSearchError(err.message || 'Failed to search trains');
    }
  }

  searchBtn?.addEventListener('click', () => {
    triggerSearch();
  });
}
