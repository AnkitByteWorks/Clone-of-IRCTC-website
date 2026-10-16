import { store } from '../state/store.js';
import { UserApi } from '../api/client.js';

export function renderMasterListView(container) {
  const { currentUser } = store.getState();

  if (!currentUser) {
    container.innerHTML = `
      <div class="max-w-md mx-auto px-4 py-16 text-center">
        <h3 class="text-xl font-bold text-white mb-2">Sign in to manage Master Passenger List</h3>
        <p class="text-xs text-slate-400 mb-6">Store frequent travelers for 1-click lightning-fast Tatkal quota bookings.</p>
        <button id="masterlist-signin-btn" class="px-6 py-2.5 rounded-xl font-bold text-xs bg-cyan-500 text-slate-950 hover:bg-cyan-400 transition">
          Sign In Now
        </button>
      </div>
    `;
    container.querySelector('#masterlist-signin-btn')?.addEventListener('click', () => {
      store.setAuthTab('login');
      store.openModal('auth');
    });
    return;
  }

  container.innerHTML = `
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8 pb-4 border-b border-slate-800">
        <div>
          <h2 class="text-2xl font-extrabold text-white">Master Passenger List</h2>
          <p class="text-xs text-slate-400 mt-1">Pre-saved traveler profiles for 1-click Tatkal auto-fill.</p>
        </div>
        <button id="toggle-add-passenger-btn" class="px-4 py-2 rounded-xl bg-cyan-500 hover:bg-cyan-400 text-slate-950 text-xs font-bold transition flex items-center gap-1.5 self-start shadow-md shadow-cyan-500/20">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          Add New Passenger
        </button>
      </div>

      <!-- Add Passenger Form (collapsible) -->
      <div id="add-passenger-panel" class="hidden mb-8 glass-panel-elevated rounded-2xl p-6 border border-slate-700 animate-fade-in">
        <h3 class="text-sm font-bold text-white mb-4">Add Passenger to Master List</h3>
        <form id="add-passenger-form" class="space-y-4">
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label class="block text-xs font-medium text-slate-300 mb-1">Full Legal Name</label>
              <input 
                type="text" 
                name="passengerName" 
                required 
                placeholder="e.g. Ramesh Kumar"
                class="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-xs text-white placeholder-slate-600 focus:outline-none focus:border-cyan-500"
              />
            </div>
            <div>
              <label class="block text-xs font-medium text-slate-300 mb-1">Age (Years)</label>
              <input 
                type="number" 
                name="age" 
                min="1" 
                max="120" 
                required 
                placeholder="32"
                class="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-xs text-white placeholder-slate-600 focus:outline-none focus:border-cyan-500"
              />
            </div>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label class="block text-xs font-medium text-slate-300 mb-1">Gender</label>
              <select name="gender" class="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-xs text-white focus:outline-none focus:border-cyan-500">
                <option value="MALE">Male</option>
                <option value="FEMALE">Female</option>
                <option value="OTHER">Other</option>
              </select>
            </div>
            <div>
              <label class="block text-xs font-medium text-slate-300 mb-1">Berth Preference</label>
              <select name="berthPreference" class="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-xs text-white focus:outline-none focus:border-cyan-500">
                <option value="NO_PREFERENCE">No Preference</option>
                <option value="LOWER">Lower Berth</option>
                <option value="MIDDLE">Middle Berth</option>
                <option value="UPPER">Upper Berth</option>
                <option value="SIDE_LOWER">Side Lower</option>
                <option value="SIDE_UPPER">Side Upper</option>
                <option value="WINDOW">Window Seat (CC/EC)</option>
              </select>
            </div>
          </div>

          <div class="flex items-center gap-2 pt-2">
            <input type="checkbox" id="seniorCitizenOptIn" name="seniorCitizenOptIn" class="rounded bg-slate-950 border-slate-800 text-cyan-500 focus:ring-0" />
            <label for="seniorCitizenOptIn" class="text-xs text-slate-300">Opt-in for Senior Citizen Lower Berth Quota (if eligible)</label>
          </div>

          <div class="flex justify-end gap-3 pt-3 border-t border-slate-800">
            <button type="button" id="cancel-add-btn" class="px-4 py-2 rounded-xl text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-300 transition">
              Cancel
            </button>
            <button type="submit" class="px-5 py-2 rounded-xl text-xs font-bold bg-cyan-500 hover:bg-cyan-400 text-slate-950 transition">
              Save Passenger
            </button>
          </div>
        </form>
      </div>

      <!-- Passengers Table / List Container -->
      <div id="passengers-list-container">
        <div class="text-center py-12 text-slate-500">
          Loading saved passengers...
        </div>
      </div>

    </div>
  `;

  const addPanel = container.querySelector('#add-passenger-panel');
  const toggleBtn = container.querySelector('#toggle-add-passenger-btn');
  const cancelBtn = container.querySelector('#cancel-add-btn');
  const form = container.querySelector('#add-passenger-form');
  const listContainer = container.querySelector('#passengers-list-container');

  toggleBtn?.addEventListener('click', () => {
    addPanel.classList.toggle('hidden');
  });

  cancelBtn?.addEventListener('click', () => {
    addPanel.classList.add('hidden');
    form.reset();
  });

  form?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const data = {
      passengerName: form.passengerName.value.trim(),
      age: parseInt(form.age.value, 10),
      gender: form.gender.value,
      berthPreference: form.berthPreference.value,
      seniorCitizenOptIn: form.seniorCitizenOptIn.checked,
    };

    try {
      await UserApi.addSavedPassenger(data);
      form.reset();
      addPanel.classList.add('hidden');
      loadPassengers();
    } catch (err) {
      alert(`Failed to save passenger: ${err.message}`);
    }
  });

  async function loadPassengers() {
    try {
      const passengers = await UserApi.getSavedPassengers();
      if (!passengers || passengers.length === 0) {
        listContainer.innerHTML = `
          <div class="glass-panel rounded-2xl p-10 text-center">
            <p class="text-slate-400 text-sm">No saved passengers found in your master list.</p>
            <p class="text-xs text-slate-500 mt-1">Add your family and frequent co-travelers to save precious seconds during Tatkal booking.</p>
          </div>
        `;
        return;
      }

      listContainer.innerHTML = `
        <div class="space-y-3">
          ${passengers.map(p => `
            <div class="glass-panel rounded-xl p-4 border border-slate-800 flex items-center justify-between hover:border-slate-700 transition">
              <div class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-center text-cyan-400 font-bold text-xs">
                  ${p.passengerName.slice(0, 2).toUpperCase()}
                </div>
                <div>
                  <h4 class="text-sm font-bold text-white">${p.passengerName}</h4>
                  <p class="text-xs text-slate-400">${p.age} years • ${p.gender} • Pref: <span class="text-cyan-400 font-mono">${p.berthPreference}</span></p>
                </div>
              </div>

              <button data-delete-id="${p.id}" class="delete-passenger-btn p-2 rounded-lg text-slate-400 hover:text-red-400 hover:bg-slate-900 transition" title="Delete Passenger">
                <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/></svg>
              </button>
            </div>
          `).join('')}
        </div>
      `;

      listContainer.querySelectorAll('.delete-passenger-btn').forEach(btn => {
        btn.addEventListener('click', async () => {
          const id = btn.getAttribute('data-delete-id');
          if (!confirm('Remove this passenger from your master list?')) return;
          try {
            await UserApi.deleteSavedPassenger(id);
            loadPassengers();
          } catch (err) {
            alert(`Failed to delete passenger: ${err.message}`);
          }
        });
      });

    } catch (err) {
      listContainer.innerHTML = `
        <div class="p-4 rounded-xl bg-red-500/10 border border-red-500/30 text-xs text-red-400">
          ${err.message || 'Failed to load master passenger list'}
        </div>
      `;
    }
  }

  loadPassengers();
}
