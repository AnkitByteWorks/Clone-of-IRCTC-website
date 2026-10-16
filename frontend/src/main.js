import './style.css';
import { store } from './state/store.js';
import { SystemApi, CatalogApi, InventoryApi, NotificationApi } from './api/client.js';
import { renderNavbar } from './components/Navbar.js';
import { renderHeroSearch } from './components/HeroSearch.js';
import { renderSearchResults } from './components/SearchResults.js';
import { renderAuthModal } from './components/AuthModal.js';
import { renderRouteModal } from './components/RouteModal.js';
import { renderCoachPickerModal } from './components/CoachPickerModal.js';
import { renderBookingModal } from './components/BookingModal.js';
import { renderPnrTicketModal } from './components/PnrTicketCard.js';
import { renderNotificationDrawer } from './components/NotificationDrawer.js';
import { renderPnrView } from './components/PnrView.js';
import { renderMyBookingsView } from './components/MyBookingsView.js';
import { renderMasterListView } from './components/MasterListView.js';

const app = document.getElementById('app');

// App Root Layout Scaffold
app.innerHTML = `
  <div id="navbar-container"></div>
  <main id="main-content" class="flex-1 flex flex-col"></main>
  <div id="modal-container"></div>

  <!-- Footer -->
  <footer class="mt-auto border-t border-slate-900 bg-slate-950/90 py-8 text-xs text-slate-500">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-4">
      <div class="flex items-center gap-2">
        <span class="font-extrabold text-white font-mono">Rail<span class="text-cyan-400">Connect</span></span>
        <span>•</span>
        <span>High-Speed Reactive Indian Railways Ticketing Engine</span>
      </div>
      <div class="flex items-center gap-4 text-slate-400 font-mono text-[11px]">
        <span>Java 25 Virtual Threads</span>
        <span>•</span>
        <span>MySQL Pessimistic Locking</span>
        <span>•</span>
        <span>Redis Atomicity</span>
        <span>•</span>
        <span>Apache Kafka</span>
      </div>
    </div>
  </footer>
`;

const navbarContainer = document.getElementById('navbar-container');
const mainContent = document.getElementById('main-content');
const modalContainer = document.getElementById('modal-container');

// Render functions
function renderApp() {
  const state = store.getState();

  // 1. Render Top Navigation Bar
  renderNavbar(navbarContainer);

  // 2. Render Active Tab View
  if (state.activeTab === 'search') {
    mainContent.innerHTML = `
      <div id="hero-search-container"></div>
      <div id="search-results-container"></div>
    `;
    const heroContainer = document.getElementById('hero-search-container');
    const resultsContainer = document.getElementById('search-results-container');
    renderHeroSearch(heroContainer);
    renderSearchResults(resultsContainer);
  } else if (state.activeTab === 'pnr') {
    mainContent.innerHTML = `<div id="pnr-container"></div>`;
    renderPnrView(document.getElementById('pnr-container'));
  } else if (state.activeTab === 'bookings') {
    mainContent.innerHTML = `<div id="bookings-container"></div>`;
    renderMyBookingsView(document.getElementById('bookings-container'));
  } else if (state.activeTab === 'masterList') {
    mainContent.innerHTML = `<div id="masterlist-container"></div>`;
    renderMasterListView(document.getElementById('masterlist-container'));
  }

  // 3. Render Modals & Slide-over Drawers
  modalContainer.innerHTML = '';
  renderAuthModal(modalContainer);
  renderRouteModal(modalContainer);
  renderCoachPickerModal(modalContainer);
  renderBookingModal(modalContainer);
  renderPnrTicketModal(modalContainer);
  renderNotificationDrawer(modalContainer);
}

// Subscribe to state updates
store.subscribe(() => {
  renderApp();
});

// Initial System Infrastructure Health Check
async function checkInfrastructure() {
  try {
    const statusData = await SystemApi.getStatus();
    store.setInfrastructureStatus({
      mysql: statusData.mysql || 'DOWN',
      mongodb: statusData.mongodb || 'DOWN',
      redis: statusData.redis || 'DOWN',
      kafka: statusData.kafka || 'DOWN',
    });
  } catch (err) {
    console.warn('System status check skipped/offline:', err);
    store.setInfrastructureStatus({
      mysql: 'DOWN',
      mongodb: 'DOWN',
      redis: 'DOWN',
      kafka: 'DOWN',
    });
  }
}

// Initial Train Search to populate default popular route (PNBE -> NDLS)
async function initialDefaultSearch() {
  const s = store.getState().searchParams;
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

// Check user notifications if logged in
async function loadUserNotifications() {
  if (store.getState().currentUser) {
    try {
      const notifs = await NotificationApi.getMyNotifications();
      store.setNotifications(notifs || []);
    } catch {
      // ignore
    }
  }
}

// Kickoff
renderApp();
checkInfrastructure();
initialDefaultSearch();
loadUserNotifications();

// Periodic infra check every 30 seconds
setInterval(checkInfrastructure, 30000);

// Auto-poll Kafka notifications every 15 seconds
setInterval(loadUserNotifications, 15000);
