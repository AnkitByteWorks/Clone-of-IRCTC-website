import { store } from '../state/store.js';

export function renderRouteModal(container) {
  const state = store.getState();
  if (state.activeModal !== 'route' || !state.activeRouteTrain) {
    container.innerHTML = '';
    return;
  }

  const train = state.activeRouteTrain;
  const { from, to } = state.searchParams;
  const routeStops = train.routeStops || [];

  container.innerHTML = `
    <div id="route-modal-backdrop" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-fade-in">
      <div id="route-modal-card" class="w-full max-w-3xl rounded-2xl glass-panel-elevated p-6 relative overflow-hidden shadow-2xl border border-slate-800 bg-slate-900/95 flex flex-col max-h-[90vh]">
        
        <!-- Header -->
        <div class="flex items-start justify-between pb-4 border-b border-slate-800 flex-shrink-0">
          <div>
            <div class="flex items-center gap-2.5">
              <span class="px-2.5 py-0.5 rounded-lg bg-cyan-950 text-cyan-400 border border-cyan-800/60 font-mono text-sm font-bold">
                #${train.trainNumber}
              </span>
              <h2 class="text-lg sm:text-xl font-bold text-white">${train.trainName}</h2>
            </div>
            <p class="text-xs text-slate-400 mt-1 flex items-center gap-2">
              <span>Complete Route & Station Timetable</span>
              <span>•</span>
              <span class="font-mono text-cyan-400">${routeStops.length} Intermediate Stoppages</span>
            </p>
          </div>

          <button id="close-route-btn" class="text-slate-400 hover:text-white p-1.5 rounded-lg hover:bg-slate-800/80 transition">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>
          </button>
        </div>

        <!-- Legend -->
        <div class="flex flex-wrap items-center gap-4 py-2.5 px-3 bg-slate-950/60 rounded-xl my-3 text-[11px] text-slate-400 flex-shrink-0 border border-slate-800/60">
          <div class="flex items-center gap-1.5">
            <span class="w-2.5 h-2.5 rounded-full bg-cyan-400"></span>
            <span>Your Boarding Station</span>
          </div>
          <div class="flex items-center gap-1.5">
            <span class="w-2.5 h-2.5 rounded-full bg-emerald-400"></span>
            <span>Your Destination Station</span>
          </div>
          <div class="flex items-center gap-1.5">
            <span class="w-2.5 h-2.5 rounded-full bg-slate-600"></span>
            <span>Intermediate En-Route Stop</span>
          </div>
        </div>

        <!-- Scrollable Vertical Timeline -->
        <div class="overflow-y-auto pr-2 space-y-3 flex-1">
          <div class="relative border-l-2 border-slate-800 ml-4 pl-6 space-y-6 my-2">
            ${routeStops.map((stop, idx) => {
              const isBoarding = stop.stationCode === from;
              const isDestination = stop.stationCode === to;
              const isSource = idx === 0;
              const isTerminus = idx === routeStops.length - 1;

              let nodeColor = 'bg-slate-700 border-slate-900';
              if (isBoarding) nodeColor = 'bg-cyan-400 border-cyan-200 ring-4 ring-cyan-500/20';
              else if (isDestination) nodeColor = 'bg-emerald-400 border-emerald-200 ring-4 ring-emerald-500/20';
              else if (isSource) nodeColor = 'bg-blue-500 border-slate-900';
              else if (isTerminus) nodeColor = 'bg-purple-500 border-slate-900';

              return `
                <div class="relative group">
                  
                  <!-- Timeline Node Dot -->
                  <div class="absolute -left-[31px] top-1.5 w-4 h-4 rounded-full border-2 ${nodeColor} transition-all"></div>

                  <!-- Stop Card -->
                  <div class="p-3.5 rounded-xl border ${isBoarding ? 'bg-cyan-950/20 border-cyan-500/40' : isDestination ? 'bg-emerald-950/20 border-emerald-500/40' : 'bg-slate-950/80 border-slate-800/80'} hover:border-slate-700 transition">
                    
                    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                      <div class="flex items-center gap-2.5">
                        <span class="px-2 py-0.5 rounded font-mono text-xs font-bold ${isBoarding ? 'bg-cyan-500 text-slate-950' : isDestination ? 'bg-emerald-500 text-slate-950' : 'bg-slate-900 text-slate-300 border border-slate-800'}">
                          ${stop.stationCode}
                        </span>
                        <div>
                          <h4 class="text-sm font-bold text-white flex items-center gap-2">
                            <span>${stop.stationName || stop.stationCode}</span>
                            ${isBoarding ? '<span class="px-2 py-0.2 rounded text-[10px] font-extrabold bg-cyan-400 text-slate-950 uppercase">Boarding</span>' : ''}
                            ${isDestination ? '<span class="px-2 py-0.2 rounded text-[10px] font-extrabold bg-emerald-400 text-slate-950 uppercase">Destination</span>' : ''}
                          </h4>
                          <p class="text-[11px] text-slate-400">Stop #${stop.stopNumber || (idx + 1)} • Platform ${stop.platformNumber || 1}</p>
                        </div>
                      </div>

                      <!-- Arrival / Departure / Halt / Distance -->
                      <div class="flex items-center gap-4 text-xs font-mono">
                        <div class="text-right">
                          <p class="text-slate-400 text-[10px] uppercase">Arr / Dep</p>
                          <p class="text-white font-bold">
                            ${stop.arrivalTime || 'Source'} → ${stop.departureTime || 'Terminus'}
                          </p>
                        </div>

                        <div class="text-right border-l border-slate-800 pl-3">
                          <p class="text-slate-400 text-[10px] uppercase">Halt</p>
                          <p class="text-cyan-400 font-bold">${stop.haltDurationMinutes ? `${stop.haltDurationMinutes} min` : 'Origin'}</p>
                        </div>

                        <div class="text-right border-l border-slate-800 pl-3">
                          <p class="text-slate-400 text-[10px] uppercase">Distance</p>
                          <p class="text-slate-300">${stop.distanceFromSourceKm || 0} km</p>
                        </div>
                      </div>

                    </div>

                  </div>
                </div>
              `;
            }).join('')}
          </div>
        </div>

        <!-- Footer -->
        <div class="pt-4 border-t border-slate-800/80 text-right flex-shrink-0">
          <button id="close-route-footer-btn" class="px-5 py-2 rounded-xl text-xs font-bold bg-slate-800 hover:bg-slate-700 text-white transition">
            Close Timetable
          </button>
        </div>

      </div>
    </div>
  `;

  const backdrop = container.querySelector('#route-modal-backdrop');
  const closeBtn = container.querySelector('#close-route-btn');
  const closeFooterBtn = container.querySelector('#close-route-footer-btn');

  closeBtn?.addEventListener('click', () => store.closeModal());
  closeFooterBtn?.addEventListener('click', () => store.closeModal());
  backdrop?.addEventListener('click', (e) => {
    if (e.target === backdrop) store.closeModal();
  });
}
