import { store } from '../state/store.js';
import { CatalogApi } from '../api/client.js';

export function renderSearchResults(container) {
  const state = store.getState();
  const { searchResults, liveAvailabilities, selectedQuota, searchParams, isSearching, searchError } = state;

  if (isSearching) {
    container.innerHTML = `
      <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-16 text-center">
        <div class="inline-flex w-16 h-16 rounded-2xl bg-cyan-500/10 border border-cyan-500/30 items-center justify-center text-cyan-400 mb-4 animate-bounce">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-8 h-8 animate-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 2a10 10 0 0 1 10 10"/></svg>
        </div>
        <h3 class="text-xl font-bold text-white">Querying Live Railway Timetables & ACID Quotas...</h3>
        <p class="text-slate-400 text-sm mt-1">Connecting to MongoDB Train Schedules and MySQL Seat Engine</p>
      </div>
    `;
    return;
  }

  if (searchError) {
    container.innerHTML = `
      <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div class="p-6 rounded-2xl bg-red-500/10 border border-red-500/30 text-center max-w-xl mx-auto">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-10 h-10 text-red-400 mx-auto mb-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
          <h3 class="text-lg font-bold text-white mb-1">Search Failed</h3>
          <p class="text-xs text-red-300">${searchError}</p>
        </div>
      </div>
    `;
    return;
  }

  if (!searchResults || searchResults.length === 0) {
    container.innerHTML = `
      <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-16 text-center">
        <div class="w-16 h-16 rounded-2xl bg-slate-900 border border-slate-800 flex items-center justify-center text-slate-500 mx-auto mb-4">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-8 h-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect width="16" height="16" x="4" y="3" rx="2"/><path d="M4 11h16"/><path d="M12 3v8"/></svg>
        </div>
        <h3 class="text-lg font-bold text-white">Ready to Explore</h3>
        <p class="text-slate-400 text-xs sm:text-sm mt-1 max-w-md mx-auto">
          Enter origin and destination stations above or pick a popular route to query live train availability.
        </p>
      </div>
    `;
    return;
  }

  // Format DayOfWeek array into 'M T W T F S S'
  const allDays = [
    { key: 'MONDAY', label: 'M' },
    { key: 'TUESDAY', label: 'T' },
    { key: 'WEDNESDAY', label: 'W' },
    { key: 'THURSDAY', label: 'T' },
    { key: 'FRIDAY', label: 'F' },
    { key: 'SATURDAY', label: 'S' },
    { key: 'SUNDAY', label: 'S' },
  ];

  function formatDuration(minutes) {
    const hrs = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return `${hrs}h ${mins > 0 ? `${mins}m` : ''}`;
  }

  function getTrainTypeBadge(type) {
    switch (type) {
      case 'VANDE_BHARAT':
        return `<span class="px-2.5 py-0.5 rounded-full text-[10px] font-extrabold bg-gradient-to-r from-cyan-500 to-blue-600 text-slate-950 uppercase tracking-wider">Vande Bharat ⚡</span>`;
      case 'RAJDHANI':
        return `<span class="px-2.5 py-0.5 rounded-full text-[10px] font-extrabold bg-purple-500/20 text-purple-300 border border-purple-500/40 uppercase tracking-wider">Rajdhani Express</span>`;
      case 'SHATABDI':
        return `<span class="px-2.5 py-0.5 rounded-full text-[10px] font-extrabold bg-blue-500/20 text-blue-300 border border-blue-500/40 uppercase tracking-wider">Shatabdi Express</span>`;
      case 'SUPERFAST':
        return `<span class="px-2.5 py-0.5 rounded-full text-[10px] font-extrabold bg-amber-500/20 text-amber-300 border border-amber-500/40 uppercase tracking-wider">Superfast</span>`;
      default:
        return `<span class="px-2.5 py-0.5 rounded-full text-[10px] font-extrabold bg-slate-800 text-slate-300 border border-slate-700 uppercase tracking-wider">${type || 'Express'}</span>`;
    }
  }

  function getStatusBadge(statusStr) {
    if (!statusStr) {
      return `<span class="text-slate-500 text-xs font-mono">CHECKING...</span>`;
    }

    if (statusStr.startsWith('AVAILABLE')) {
      return `<span class="px-2 py-0.5 rounded-md font-mono text-xs font-bold bg-emerald-500/15 text-emerald-400 border border-emerald-500/30">${statusStr}</span>`;
    } else if (statusStr.startsWith('RAC')) {
      return `<span class="px-2 py-0.5 rounded-md font-mono text-xs font-bold bg-amber-500/15 text-amber-400 border border-amber-500/30">${statusStr}</span>`;
    } else if (statusStr.startsWith('WL')) {
      return `<span class="px-2 py-0.5 rounded-md font-mono text-xs font-bold bg-orange-500/15 text-orange-400 border border-orange-500/30">${statusStr}</span>`;
    } else {
      return `<span class="px-2 py-0.5 rounded-md font-mono text-xs font-bold bg-red-500/15 text-red-400 border border-red-500/30">${statusStr}</span>`;
    }
  }

  container.innerHTML = `
    <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      
      <!-- Results Header -->
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 mb-6">
        <div>
          <h2 class="text-xl sm:text-2xl font-bold text-white flex items-center gap-2">
            <span>${searchResults.length} Trains Found</span>
            <span class="text-sm font-normal text-slate-400">between ${searchParams.from} and ${searchParams.to}</span>
          </h2>
          <p class="text-xs text-slate-500 mt-0.5">Showing live availability for <strong>${selectedQuota}</strong> quota on ${searchParams.date}</p>
        </div>
      </div>

      <!-- Train Cards List -->
      <div class="space-y-6">
        ${searchResults.map(train => {
          const avail = liveAvailabilities[train.trainNumber];
          const quotasList = avail?.quotas || [];

          return `
            <div class="glass-panel rounded-2xl p-6 hover:border-slate-700 transition shadow-xl border border-slate-800">
              
              <!-- Train Header Info -->
              <div class="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-4 border-b border-slate-800/80">
                <div class="flex flex-wrap items-center gap-3">
                  <span class="px-2.5 py-1 rounded-xl bg-slate-900 border border-slate-700 font-mono text-sm font-bold text-cyan-400 shadow-inner">
                    #${train.trainNumber}
                  </span>
                  <h3 class="text-base sm:text-lg font-bold text-white">${train.trainName}</h3>
                  ${getTrainTypeBadge(train.trainType)}
                </div>

                <!-- Running Days -->
                <div class="flex items-center gap-3">
                  <div class="flex items-center gap-1">
                    <span class="text-[10px] text-slate-500 uppercase mr-1">Runs On:</span>
                    ${allDays.map(d => {
                      const isActive = train.runningDays && train.runningDays.includes(d.key);
                      return `
                        <span class="w-5 h-5 rounded flex items-center justify-center text-[10px] font-mono font-bold ${isActive ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40' : 'bg-slate-900 text-slate-600'}">
                          ${d.label}
                        </span>
                      `;
                    }).join('')}
                  </div>

                  <button 
                    data-train="${train.trainNumber}" 
                    class="view-route-btn px-3 py-1.5 rounded-xl text-xs font-semibold bg-slate-900 hover:bg-slate-800 border border-slate-700 text-slate-300 hover:text-cyan-400 transition flex items-center gap-1.5"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="6" cy="19" r="3"/><path d="M9 19h8.5a3.5 3.5 0 0 0 0-7h-11a3.5 3.5 0 0 1 0-7H15"/><circle cx="18" cy="5" r="3"/></svg>
                    <span>View Route & Schedule</span>
                  </button>
                </div>
              </div>

              <!-- Schedule Times & Stations -->
              <div class="py-5 grid grid-cols-1 md:grid-cols-3 gap-4 items-center">
                <!-- Departure -->
                <div class="text-left">
                  <p class="text-2xl font-extrabold text-white font-mono">${train.departureTime || '06:00'}</p>
                  <p class="text-sm font-semibold text-slate-200 mt-0.5">${train.fromStationName} (${train.fromStationCode})</p>
                  <p class="text-xs text-slate-400">Boarding Station</p>
                </div>

                <!-- Journey Duration & Path -->
                <div class="flex flex-col items-center justify-center text-center">
                  <span class="text-xs font-mono text-cyan-400 font-semibold mb-1">${formatDuration(train.durationMinutes || 735)}</span>
                  <div class="w-full flex items-center justify-center gap-2">
                    <div class="w-2.5 h-2.5 rounded-full bg-cyan-400 border border-slate-950"></div>
                    <div class="h-0.5 flex-1 bg-gradient-to-r from-cyan-500 via-blue-500 to-emerald-400 rounded-full"></div>
                    <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m9 18 6-6-6-6"/></svg>
                    <div class="h-0.5 flex-1 bg-gradient-to-r from-cyan-500 via-blue-500 to-emerald-400 rounded-full"></div>
                    <div class="w-2.5 h-2.5 rounded-full bg-emerald-400 border border-slate-950"></div>
                  </div>
                  <span class="text-[11px] text-slate-500 font-mono mt-1">${train.distanceKm || 998} km</span>
                </div>

                <!-- Arrival -->
                <div class="text-left md:text-right">
                  <p class="text-2xl font-extrabold text-white font-mono">${train.arrivalTime || '18:15'}</p>
                  <p class="text-sm font-semibold text-slate-200 mt-0.5">${train.toStationName} (${train.toStationCode})</p>
                  <p class="text-xs text-slate-400">Destination Station</p>
                </div>
              </div>

              <!-- Available Class Cards -->
              <div class="pt-4 border-t border-slate-800/80">
                <p class="text-[11px] font-semibold text-slate-400 uppercase tracking-wider mb-3">Live Class Availability (${selectedQuota}):</p>
                <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-6 gap-3">
                  ${(train.availableClasses || []).map(cls => {
                    const quotaMatch = quotasList.find(q => q.classCode === cls.classCode && q.quotaType === selectedQuota);
                    const displayStatus = quotaMatch ? quotaMatch.displayStatus : 'CHECKING...';
                    const fare = selectedQuota === 'TATKAL' && cls.tatkalFare ? cls.tatkalFare : cls.generalFare;

                    return `
                      <div 
                        data-train-num="${train.trainNumber}"
                        data-train-name="${train.trainName}"
                        data-from="${train.fromStationCode}"
                        data-to="${train.toStationCode}"
                        data-dep="${train.departureTime || '06:00'}"
                        data-arr="${train.arrivalTime || '18:15'}"
                        data-class="${cls.classCode}"
                        data-fare="${fare || 0}"
                        class="class-card group p-3 rounded-xl bg-slate-950 hover:bg-slate-900 border border-slate-800 hover:border-cyan-500/50 cursor-pointer transition-all flex flex-col justify-between shadow-sm hover:shadow-cyan-500/10"
                      >
                        <div class="flex items-center justify-between mb-2">
                          <span class="px-2 py-0.5 rounded font-mono font-bold text-xs bg-slate-900 text-white border border-slate-800 group-hover:border-cyan-500/30">
                            ${cls.classCode}
                          </span>
                          <span class="text-[10px] text-slate-500 font-mono">${cls.totalSeats || 64} seats</span>
                        </div>

                        <div class="my-1.5">
                          ${getStatusBadge(displayStatus)}
                        </div>

                        <div class="mt-2 pt-2 border-t border-slate-900 flex items-center justify-between">
                          <span class="text-sm font-extrabold text-white font-mono">₹${fare || 0}</span>
                          <span class="text-[10px] text-cyan-400 font-semibold group-hover:translate-x-0.5 transition-transform">Select Berths →</span>
                        </div>
                      </div>
                    `;
                  }).join('')}
                </div>
              </div>

            </div>
          `;
        }).join('')}
      </div>

    </div>
  `;

  // Attach event listener to view route buttons
  container.querySelectorAll('.view-route-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      const trainNumber = btn.getAttribute('data-train');
      try {
        const fullTrainDoc = await CatalogApi.getTrain(trainNumber);
        store.openModal('route', fullTrainDoc);
      } catch (err) {
        alert(`Failed to load route schedule: ${err.message}`);
      }
    });
  });

  // Attach event listeners to class cards to open CoachPickerModal
  container.querySelectorAll('.class-card').forEach(card => {
    card.addEventListener('click', () => {
      const trainNumber = card.getAttribute('data-train-num');
      const trainName = card.getAttribute('data-train-name');
      const fromStationCode = card.getAttribute('data-from');
      const toStationCode = card.getAttribute('data-to');
      const departureTime = card.getAttribute('data-dep');
      const arrivalTime = card.getAttribute('data-arr');
      const classCode = card.getAttribute('data-class');
      const fare = parseFloat(card.getAttribute('data-fare') || 0);

      store.openCoachPicker({
        trainNumber,
        trainName,
        fromStationCode,
        toStationCode,
        journeyDate: store.getState().searchParams.date,
        classCode,
        fare,
        departureTime,
        arrivalTime,
      });
    });
  });
}
