// Comprehensive Indian Railways Dataset Generator (302 Stations, Exactly 1000 Popular Trains)
// Guarantees rich, multi-train connectivity across all major corridors (e.g. HWH <-> DDU, NDLS <-> CNB, etc.)
// Preserves existing 4 test trains (12309, 12310, 22436, 12301) and all 10 original stations.
// Does NOT include station 'BPL' (reserved for CatalogControllerIntegrationTest).
// Enforces that on Monday 2026-10-19, PNBE -> NDLS matches ONLY 12309.

const fs = require('fs');
const path = require('path');

const STATIONS_FILE = path.join(__dirname, 'stations.json');
const TRAINS_FILE = path.join(__dirname, 'trains.json');

// --- 1. DEFINE 302 REAL INDIAN RAILWAY STATIONS ---
const STATIONS = [
  // Original 10 Stations (Must be preserved exactly)
  { stationCode: "PNBE", stationName: "Patna Junction", city: "Patna", state: "Bihar", zone: "ECR", numberOfPlatforms: 10, active: true },
  { stationCode: "DDU", stationName: "Pt. Deen Dayal Upadhyaya Jn", city: "Mughalsarai", state: "Uttar Pradesh", zone: "ECR", numberOfPlatforms: 8, active: true },
  { stationCode: "PRYJ", stationName: "Prayagraj Junction", city: "Prayagraj", state: "Uttar Pradesh", zone: "NCR", numberOfPlatforms: 10, active: true },
  { stationCode: "CNB", stationName: "Kanpur Central", city: "Kanpur", state: "Uttar Pradesh", zone: "NCR", numberOfPlatforms: 10, active: true },
  { stationCode: "NDLS", stationName: "New Delhi", city: "New Delhi", state: "Delhi", zone: "NR", numberOfPlatforms: 16, active: true },
  { stationCode: "BSB", stationName: "Varanasi Junction", city: "Varanasi", state: "Uttar Pradesh", zone: "NR", numberOfPlatforms: 9, active: true },
  { stationCode: "HWH", stationName: "Howrah Junction", city: "Kolkata", state: "West Bengal", zone: "ER", numberOfPlatforms: 23, active: true },
  { stationCode: "DHN", stationName: "Dhanbad Junction", city: "Dhanbad", state: "Jharkhand", zone: "ECR", numberOfPlatforms: 8, active: true },
  { stationCode: "GAYA", stationName: "Gaya Junction", city: "Gaya", state: "Bihar", zone: "ECR", numberOfPlatforms: 9, active: true },
  { stationCode: "BCT", stationName: "Mumbai Central", city: "Mumbai", state: "Maharashtra", zone: "WR", numberOfPlatforms: 5, active: true },

  // Delhi NCR & Haryana & Punjab & J&K
  { stationCode: "DLI", stationName: "Old Delhi Junction", city: "Delhi", state: "Delhi", zone: "NR", numberOfPlatforms: 16, active: true },
  { stationCode: "NZM", stationName: "Hazrat Nizamuddin", city: "New Delhi", state: "Delhi", zone: "NR", numberOfPlatforms: 9, active: true },
  { stationCode: "ANVT", stationName: "Anand Vihar Terminal", city: "Delhi", state: "Delhi", zone: "NR", numberOfPlatforms: 7, active: true },
  { stationCode: "DEC", stationName: "Delhi Cantt", city: "Delhi", state: "Delhi", zone: "NR", numberOfPlatforms: 4, active: true },
  { stationCode: "GZB", stationName: "Ghaziabad Junction", city: "Ghaziabad", state: "Uttar Pradesh", zone: "NR", numberOfPlatforms: 6, active: true },
  { stationCode: "FDB", stationName: "Faridabad", city: "Faridabad", state: "Haryana", zone: "NR", numberOfPlatforms: 4, active: true },
  { stationCode: "PNP", stationName: "Panipat Junction", city: "Panipat", state: "Haryana", zone: "NR", numberOfPlatforms: 5, active: true },
  { stationCode: "KUN", stationName: "Karnal", city: "Karnal", state: "Haryana", zone: "NR", numberOfPlatforms: 3, active: true },
  { stationCode: "KKDE", stationName: "Kurukshetra Junction", city: "Kurukshetra", state: "Haryana", zone: "NR", numberOfPlatforms: 3, active: true },
  { stationCode: "UMB", stationName: "Ambala Cantt Junction", city: "Ambala", state: "Haryana", zone: "NR", numberOfPlatforms: 8, active: true },
  { stationCode: "CDG", stationName: "Chandigarh Junction", city: "Chandigarh", state: "Chandigarh", zone: "NR", numberOfPlatforms: 6, active: true },
  { stationCode: "KLK", stationName: "Kalka", city: "Kalka", state: "Haryana", zone: "NR", numberOfPlatforms: 4, active: true },
  { stationCode: "LDH", stationName: "Ludhiana Junction", city: "Ludhiana", state: "Punjab", zone: "NR", numberOfPlatforms: 7, active: true },
  { stationCode: "PGW", stationName: "Phagwara Junction", city: "Phagwara", state: "Punjab", zone: "NR", numberOfPlatforms: 3, active: true },
  { stationCode: "JUC", stationName: "Jalandhar City Junction", city: "Jalandhar", state: "Punjab", zone: "NR", numberOfPlatforms: 5, active: true },
  { stationCode: "JRC", stationName: "Jalandhar Cantt", city: "Jalandhar", state: "Punjab", zone: "NR", numberOfPlatforms: 3, active: true },
  { stationCode: "BEAS", stationName: "Beas Junction", city: "Beas", state: "Punjab", zone: "NR", numberOfPlatforms: 2, active: true },
  { stationCode: "ASR", stationName: "Amritsar Junction", city: "Amritsar", state: "Punjab", zone: "NR", numberOfPlatforms: 6, active: true },
  { stationCode: "PTKC", stationName: "Pathankot Cantt", city: "Pathankot", state: "Punjab", zone: "NR", numberOfPlatforms: 2, active: true },
  { stationCode: "KTHU", stationName: "Kathua", city: "Kathua", state: "Jammu and Kashmir", zone: "NR", numberOfPlatforms: 2, active: true },
  { stationCode: "JAT", stationName: "Jammu Tawi", city: "Jammu", state: "Jammu and Kashmir", zone: "NR", numberOfPlatforms: 4, active: true },
  { stationCode: "MCTM", stationName: "MCTM Udhampur", city: "Udhampur", state: "Jammu and Kashmir", zone: "NR", numberOfPlatforms: 3, active: true },
  { stationCode: "SVDK", stationName: "Shri Mata Vaishno Devi Katra", city: "Katra", state: "Jammu and Kashmir", zone: "NR", numberOfPlatforms: 5, active: true },
  { stationCode: "BTI", stationName: "Bathinda Junction", city: "Bathinda", state: "Punjab", zone: "NR", numberOfPlatforms: 7, active: true },
  { stationCode: "FZR", stationName: "Firozpur Cantt", city: "Firozpur", state: "Punjab", zone: "NR", numberOfPlatforms: 5, active: true },
  { stationCode: "RE", stationName: "Rewari Junction", city: "Rewari", state: "Haryana", zone: "NWR", numberOfPlatforms: 8, active: true },
  { stationCode: "GGN", stationName: "Gurugram", city: "Gurgaon", state: "Haryana", zone: "NR", numberOfPlatforms: 3, active: true },

  // Uttar Pradesh & Uttarakhand
  { stationCode: "ALJN", stationName: "Aligarh Junction", city: "Aligarh", state: "Uttar Pradesh", zone: "NCR", numberOfPlatforms: 7, active: true },
  { stationCode: "TDL", stationName: "Tundla Junction", city: "Tundla", state: "Uttar Pradesh", zone: "NCR", numberOfPlatforms: 7, active: true },
  { stationCode: "ETW", stationName: "Etawah Junction", city: "Etawah", state: "Uttar Pradesh", zone: "NCR", numberOfPlatforms: 5, active: true },
  { stationCode: "MZP", stationName: "Mirzapur", city: "Mirzapur", state: "Uttar Pradesh", zone: "NCR", numberOfPlatforms: 3, active: true },
  { stationCode: "CAR", stationName: "Chunar Junction", city: "Chunar", state: "Uttar Pradesh", zone: "NCR", numberOfPlatforms: 5, active: true },
  { stationCode: "AGC", stationName: "Agra Cantt", city: "Agra", state: "Uttar Pradesh", zone: "NCR", numberOfPlatforms: 6, active: true },
  { stationCode: "AF", stationName: "Agra Fort", city: "Agra", state: "Uttar Pradesh", zone: "NCR", numberOfPlatforms: 4, active: true },
  { stationCode: "MTJ", stationName: "Mathura Junction", city: "Mathura", state: "Uttar Pradesh", zone: "NCR", numberOfPlatforms: 10, active: true },
  { stationCode: "VGLJ", stationName: "VGL Jhansi Junction", city: "Jhansi", state: "Uttar Pradesh", zone: "NCR", numberOfPlatforms: 8, active: true },
  { stationCode: "ORAI", stationName: "Orai", city: "Orai", state: "Uttar Pradesh", zone: "NCR", numberOfPlatforms: 3, active: true },
  { stationCode: "BNDA", stationName: "Banda", city: "Banda", state: "Uttar Pradesh", zone: "NCR", numberOfPlatforms: 3, active: true },
  { stationCode: "MKP", stationName: "Manikpur Junction", city: "Manikpur", state: "Uttar Pradesh", zone: "NCR", numberOfPlatforms: 4, active: true },
  { stationCode: "MB", stationName: "Moradabad Junction", city: "Moradabad", state: "Uttar Pradesh", zone: "NR", numberOfPlatforms: 7, active: true },
  { stationCode: "BE", stationName: "Bareilly Junction", city: "Bareilly", state: "Uttar Pradesh", zone: "NR", numberOfPlatforms: 6, active: true },
  { stationCode: "SPN", stationName: "Shahjahanpur", city: "Shahjahanpur", state: "Uttar Pradesh", zone: "NR", numberOfPlatforms: 4, active: true },
  { stationCode: "HRI", stationName: "Hardoi", city: "Hardoi", state: "Uttar Pradesh", zone: "NR", numberOfPlatforms: 4, active: true },
  { stationCode: "LKO", stationName: "Lucknow Charbagh", city: "Lucknow", state: "Uttar Pradesh", zone: "NR", numberOfPlatforms: 9, active: true },
  { stationCode: "LJN", stationName: "Lucknow Junction", city: "Lucknow", state: "Uttar Pradesh", zone: "NER", numberOfPlatforms: 6, active: true },
  { stationCode: "GTNR", stationName: "Gomti Nagar", city: "Lucknow", state: "Uttar Pradesh", zone: "NER", numberOfPlatforms: 6, active: true },
  { stationCode: "BBK", stationName: "Barabanki Junction", city: "Barabanki", state: "Uttar Pradesh", zone: "NR", numberOfPlatforms: 4, active: true },
  { stationCode: "AY", stationName: "Ayodhya Dham Junction", city: "Ayodhya", state: "Uttar Pradesh", zone: "NR", numberOfPlatforms: 6, active: true },
  { stationCode: "AYC", stationName: "Ayodhya Cantt", city: "Ayodhya", state: "Uttar Pradesh", zone: "NR", numberOfPlatforms: 5, active: true },
  { stationCode: "SLN", stationName: "Sultanpur Junction", city: "Sultanpur", state: "Uttar Pradesh", zone: "NR", numberOfPlatforms: 4, active: true },
  { stationCode: "RBL", stationName: "Rae Bareli Junction", city: "Rae Bareli", state: "Uttar Pradesh", zone: "NR", numberOfPlatforms: 5, active: true },
  { stationCode: "GD", stationName: "Gonda Junction", city: "Gonda", state: "Uttar Pradesh", zone: "NER", numberOfPlatforms: 5, active: true },
  { stationCode: "BST", stationName: "Basti", city: "Basti", state: "Uttar Pradesh", zone: "NER", numberOfPlatforms: 4, active: true },
  { stationCode: "GKP", stationName: "Gorakhpur Junction", city: "Gorakhpur", state: "Uttar Pradesh", zone: "NER", numberOfPlatforms: 10, active: true },
  { stationCode: "DEOS", stationName: "Deoria Sadar", city: "Deoria", state: "Uttar Pradesh", zone: "NER", numberOfPlatforms: 3, active: true },
  { stationCode: "BTT", stationName: "Bhatni Junction", city: "Bhatni", state: "Uttar Pradesh", zone: "NER", numberOfPlatforms: 4, active: true },
  { stationCode: "MAU", stationName: "Mau Junction", city: "Mau", state: "Uttar Pradesh", zone: "NER", numberOfPlatforms: 4, active: true },
  { stationCode: "BUI", stationName: "Ballia", city: "Ballia", state: "Uttar Pradesh", zone: "NER", numberOfPlatforms: 4, active: true },
  { stationCode: "DDN", stationName: "Dehradun Terminus", city: "Dehradun", state: "Uttarakhand", zone: "NR", numberOfPlatforms: 4, active: true },
  { stationCode: "HW", stationName: "Haridwar", city: "Haridwar", state: "Uttarakhand", zone: "NR", numberOfPlatforms: 6, active: true },
  { stationCode: "RK", stationName: "Roorkee", city: "Roorkee", state: "Uttarakhand", zone: "NR", numberOfPlatforms: 3, active: true },

  // Bihar & Jharkhand
  { stationCode: "PPTA", stationName: "Patliputra Junction", city: "Patna", state: "Bihar", zone: "ECR", numberOfPlatforms: 5, active: true },
  { stationCode: "DNR", stationName: "Danapur", city: "Patna", state: "Bihar", zone: "ECR", numberOfPlatforms: 5, active: true },
  { stationCode: "RJPB", stationName: "Rajendra Nagar Terminal", city: "Patna", state: "Bihar", zone: "ECR", numberOfPlatforms: 5, active: true },
  { stationCode: "ARA", stationName: "Ara Junction", city: "Ara", state: "Bihar", zone: "ECR", numberOfPlatforms: 4, active: true },
  { stationCode: "BXR", stationName: "Buxar", city: "Buxar", state: "Bihar", zone: "ECR", numberOfPlatforms: 3, active: true },
  { stationCode: "BKP", stationName: "Bakhtiyarpur Junction", city: "Bakhtiyarpur", state: "Bihar", zone: "ECR", numberOfPlatforms: 5, active: true },
  { stationCode: "MKA", stationName: "Mokama", city: "Mokama", state: "Bihar", zone: "ECR", numberOfPlatforms: 4, active: true },
  { stationCode: "KIUL", stationName: "Kiul Junction", city: "Lakhisarai", state: "Bihar", zone: "ECR", numberOfPlatforms: 5, active: true },
  { stationCode: "JAJ", stationName: "Jhajha", city: "Jhajha", state: "Bihar", zone: "ECR", numberOfPlatforms: 4, active: true },
  { stationCode: "SSM", stationName: "Sasaram Junction", city: "Sasaram", state: "Bihar", zone: "ECR", numberOfPlatforms: 5, active: true },
  { stationCode: "DOS", stationName: "Dehri On Sone", city: "Dehri", state: "Bihar", zone: "ECR", numberOfPlatforms: 5, active: true },
  { stationCode: "CPR", stationName: "Chhapra Junction", city: "Chhapra", state: "Bihar", zone: "NER", numberOfPlatforms: 6, active: true },
  { stationCode: "SV", stationName: "Siwan Junction", city: "Siwan", state: "Bihar", zone: "NER", numberOfPlatforms: 4, active: true },
  { stationCode: "SEE", stationName: "Sonpur Junction", city: "Sonpur", state: "Bihar", zone: "ECR", numberOfPlatforms: 4, active: true },
  { stationCode: "HJP", stationName: "Hajipur Junction", city: "Hajipur", state: "Bihar", zone: "ECR", numberOfPlatforms: 5, active: true },
  { stationCode: "MFP", stationName: "Muzaffarpur Junction", city: "Muzaffarpur", state: "Bihar", zone: "ECR", numberOfPlatforms: 6, active: true },
  { stationCode: "SPJ", stationName: "Samastipur Junction", city: "Samastipur", state: "Bihar", zone: "ECR", numberOfPlatforms: 7, active: true },
  { stationCode: "DBG", stationName: "Darbhanga Junction", city: "Darbhanga", state: "Bihar", zone: "ECR", numberOfPlatforms: 5, active: true },
  { stationCode: "JYG", stationName: "Jaynagar", city: "Jaynagar", state: "Bihar", zone: "ECR", numberOfPlatforms: 4, active: true },
  { stationCode: "BJU", stationName: "Barauni Junction", city: "Barauni", state: "Bihar", zone: "ECR", numberOfPlatforms: 9, active: true },
  { stationCode: "BGS", stationName: "Begusarai", city: "Begusarai", state: "Bihar", zone: "ECR", numberOfPlatforms: 3, active: true },
  { stationCode: "KGG", stationName: "Khagaria Junction", city: "Khagaria", state: "Bihar", zone: "ECR", numberOfPlatforms: 3, active: true },
  { stationCode: "NNA", stationName: "Naugachia", city: "Naugachia", state: "Bihar", zone: "ECR", numberOfPlatforms: 2, active: true },
  { stationCode: "KIR", stationName: "Katihar Junction", city: "Katihar", state: "Bihar", zone: "NFR", numberOfPlatforms: 8, active: true },
  { stationCode: "KNE", stationName: "Kishanganj", city: "Kishanganj", state: "Bihar", zone: "NFR", numberOfPlatforms: 3, active: true },
  { stationCode: "BGP", stationName: "Bhagalpur Junction", city: "Bhagalpur", state: "Bihar", zone: "ER", numberOfPlatforms: 6, active: true },
  { stationCode: "JMP", stationName: "Jamalpur Junction", city: "Jamalpur", state: "Bihar", zone: "ER", numberOfPlatforms: 4, active: true },
  { stationCode: "MDP", stationName: "Madhupur Junction", city: "Madhupur", state: "Jharkhand", zone: "ER", numberOfPlatforms: 4, active: true },
  { stationCode: "JSME", stationName: "Jasidih Junction", city: "Deoghar", state: "Jharkhand", zone: "ER", numberOfPlatforms: 5, active: true },
  { stationCode: "KQR", stationName: "Koderma Junction", city: "Koderma", state: "Jharkhand", zone: "ECR", numberOfPlatforms: 5, active: true },
  { stationCode: "GMO", stationName: "Netaji SC Bose Gomoh", city: "Gomoh", state: "Jharkhand", zone: "ECR", numberOfPlatforms: 6, active: true },
  { stationCode: "BKSC", stationName: "Bokaro Steel City", city: "Bokaro", state: "Jharkhand", zone: "SER", numberOfPlatforms: 5, active: true },
  { stationCode: "RNC", stationName: "Ranchi Junction", city: "Ranchi", state: "Jharkhand", zone: "SER", numberOfPlatforms: 6, active: true },
  { stationCode: "HTE", stationName: "Hatia", city: "Ranchi", state: "Jharkhand", zone: "SER", numberOfPlatforms: 5, active: true },
  { stationCode: "MURI", stationName: "Muri Junction", city: "Muri", state: "Jharkhand", zone: "SER", numberOfPlatforms: 4, active: true },
  { stationCode: "TATA", stationName: "Tatanagar Junction", city: "Jamshedpur", state: "Jharkhand", zone: "SER", numberOfPlatforms: 5, active: true },
  { stationCode: "CKP", stationName: "Chakradharpur", city: "Chakradharpur", state: "Jharkhand", zone: "SER", numberOfPlatforms: 3, active: true },

  // West Bengal & North East
  { stationCode: "SDAH", stationName: "Sealdah", city: "Kolkata", state: "West Bengal", zone: "ER", numberOfPlatforms: 21, active: true },
  { stationCode: "KOAA", stationName: "Kolkata Terminus", city: "Kolkata", state: "West Bengal", zone: "ER", numberOfPlatforms: 5, active: true },
  { stationCode: "SHM", stationName: "Shalimar", city: "Howrah", state: "West Bengal", zone: "SER", numberOfPlatforms: 3, active: true },
  { stationCode: "SRC", stationName: "Santragachi Junction", city: "Howrah", state: "West Bengal", zone: "SER", numberOfPlatforms: 6, active: true },
  { stationCode: "BWN", stationName: "Barddhaman Junction", city: "Bardhaman", state: "West Bengal", zone: "ER", numberOfPlatforms: 8, active: true },
  { stationCode: "DGR", stationName: "Durgapur", city: "Durgapur", state: "West Bengal", zone: "ER", numberOfPlatforms: 5, active: true },
  { stationCode: "ASN", stationName: "Asansol Junction", city: "Asansol", state: "West Bengal", zone: "ER", numberOfPlatforms: 7, active: true },
  { stationCode: "CRJ", stationName: "Chittaranjan", city: "Chittaranjan", state: "West Bengal", zone: "ER", numberOfPlatforms: 3, active: true },
  { stationCode: "KGP", stationName: "Kharagpur Junction", city: "Kharagpur", state: "West Bengal", zone: "SER", numberOfPlatforms: 12, active: true },
  { stationCode: "MLDT", stationName: "Malda Town", city: "Malda", state: "West Bengal", zone: "ER", numberOfPlatforms: 7, active: true },
  { stationCode: "NFK", stationName: "New Farakka Junction", city: "Farakka", state: "West Bengal", zone: "ER", numberOfPlatforms: 3, active: true },
  { stationCode: "NJP", stationName: "New Jalpaiguri", city: "Siliguri", state: "West Bengal", zone: "NFR", numberOfPlatforms: 8, active: true },
  { stationCode: "NOQ", stationName: "New Alipurduar", city: "Alipurduar", state: "West Bengal", zone: "NFR", numberOfPlatforms: 3, active: true },
  { stationCode: "NBQ", stationName: "New Bongaigaon Junction", city: "Bongaigaon", state: "Assam", zone: "NFR", numberOfPlatforms: 6, active: true },
  { stationCode: "RNY", stationName: "Rangiya Junction", city: "Rangiya", state: "Assam", zone: "NFR", numberOfPlatforms: 5, active: true },
  { stationCode: "KYQ", stationName: "Kamakhya Junction", city: "Guwahati", state: "Assam", zone: "NFR", numberOfPlatforms: 4, active: true },
  { stationCode: "GHY", stationName: "Guwahati", city: "Guwahati", state: "Assam", zone: "NFR", numberOfPlatforms: 7, active: true },
  { stationCode: "LMG", stationName: "Lumding Junction", city: "Lumding", state: "Assam", zone: "NFR", numberOfPlatforms: 6, active: true },
  { stationCode: "DMV", stationName: "Dimapur", city: "Dimapur", state: "Nagaland", zone: "NFR", numberOfPlatforms: 3, active: true },
  { stationCode: "DBRG", stationName: "Dibrugarh", city: "Dibrugarh", state: "Assam", zone: "NFR", numberOfPlatforms: 5, active: true },
  { stationCode: "AGTL", stationName: "Agartala", city: "Agartala", state: "Tripura", zone: "NFR", numberOfPlatforms: 3, active: true },

  // Maharashtra & Goa
  { stationCode: "CSMT", stationName: "Chhatrapati Shivaji Maharaj Terminus", city: "Mumbai", state: "Maharashtra", zone: "CR", numberOfPlatforms: 18, active: true },
  { stationCode: "LTT", stationName: "Lokmanya Tilak Terminus", city: "Mumbai", state: "Maharashtra", zone: "CR", numberOfPlatforms: 5, active: true },
  { stationCode: "BDTS", stationName: "Bandra Terminus", city: "Mumbai", state: "Maharashtra", zone: "WR", numberOfPlatforms: 7, active: true },
  { stationCode: "DR", stationName: "Dadar Central", city: "Mumbai", state: "Maharashtra", zone: "CR", numberOfPlatforms: 8, active: true },
  { stationCode: "TNA", stationName: "Thane", city: "Thane", state: "Maharashtra", zone: "CR", numberOfPlatforms: 10, active: true },
  { stationCode: "KYN", stationName: "Kalyan Junction", city: "Kalyan", state: "Maharashtra", zone: "CR", numberOfPlatforms: 8, active: true },
  { stationCode: "PNVL", stationName: "Panvel Junction", city: "Navi Mumbai", state: "Maharashtra", zone: "CR", numberOfPlatforms: 7, active: true },
  { stationCode: "BVI", stationName: "Borivali", city: "Mumbai", state: "Maharashtra", zone: "WR", numberOfPlatforms: 10, active: true },
  { stationCode: "LNL", stationName: "Lonavala", city: "Lonavala", state: "Maharashtra", zone: "CR", numberOfPlatforms: 3, active: true },
  { stationCode: "PUNE", stationName: "Pune Junction", city: "Pune", state: "Maharashtra", zone: "CR", numberOfPlatforms: 6, active: true },
  { stationCode: "DD", stationName: "Daund Junction", city: "Daund", state: "Maharashtra", zone: "CR", numberOfPlatforms: 6, active: true },
  { stationCode: "SUR", stationName: "Solapur", city: "Solapur", state: "Maharashtra", zone: "CR", numberOfPlatforms: 5, active: true },
  { stationCode: "MRJ", stationName: "Miraj Junction", city: "Miraj", state: "Maharashtra", zone: "CR", numberOfPlatforms: 6, active: true },
  { stationCode: "KOP", stationName: "Kolhapur CSMT", city: "Kolhapur", state: "Maharashtra", zone: "CR", numberOfPlatforms: 3, active: true },
  { stationCode: "IGP", stationName: "Igatpuri", city: "Igatpuri", state: "Maharashtra", zone: "CR", numberOfPlatforms: 4, active: true },
  { stationCode: "NK", stationName: "Nashik Road", city: "Nashik", state: "Maharashtra", zone: "CR", numberOfPlatforms: 4, active: true },
  { stationCode: "MMR", stationName: "Manmad Junction", city: "Manmad", state: "Maharashtra", zone: "CR", numberOfPlatforms: 6, active: true },
  { stationCode: "JL", stationName: "Jalgaon Junction", city: "Jalgaon", state: "Maharashtra", zone: "CR", numberOfPlatforms: 5, active: true },
  { stationCode: "BSL", stationName: "Bhusawal Junction", city: "Bhusawal", state: "Maharashtra", zone: "CR", numberOfPlatforms: 8, active: true },
  { stationCode: "SEG", stationName: "Shegaon", city: "Shegaon", state: "Maharashtra", zone: "CR", numberOfPlatforms: 3, active: true },
  { stationCode: "AK", stationName: "Akola Junction", city: "Akola", state: "Maharashtra", zone: "CR", numberOfPlatforms: 6, active: true },
  { stationCode: "BD", stationName: "Badnera Junction", city: "Amravati", state: "Maharashtra", zone: "CR", numberOfPlatforms: 4, active: true },
  { stationCode: "WR", stationName: "Wardha Junction", city: "Wardha", state: "Maharashtra", zone: "CR", numberOfPlatforms: 4, active: true },
  { stationCode: "NGP", stationName: "Nagpur Junction", city: "Nagpur", state: "Maharashtra", zone: "CR", numberOfPlatforms: 8, active: true },
  { stationCode: "CD", stationName: "Chandrapur", city: "Chandrapur", state: "Maharashtra", zone: "CR", numberOfPlatforms: 3, active: true },
  { stationCode: "BPQ", stationName: "Balharshah Junction", city: "Balharshah", state: "Maharashtra", zone: "CR", numberOfPlatforms: 5, active: true },
  { stationCode: "G", stationName: "Gondia Junction", city: "Gondia", state: "Maharashtra", zone: "SECR", numberOfPlatforms: 7, active: true },
  { stationCode: "NND", stationName: "Hazur Sahib Nanded", city: "Nanded", state: "Maharashtra", zone: "SCR", numberOfPlatforms: 4, active: true },
  { stationCode: "PBN", stationName: "Parbhani Junction", city: "Parbhani", state: "Maharashtra", zone: "SCR", numberOfPlatforms: 3, active: true },
  { stationCode: "J", stationName: "Jalna", city: "Jalna", state: "Maharashtra", zone: "SCR", numberOfPlatforms: 3, active: true },
  { stationCode: "AWB", stationName: "Chhatrapati Sambhajinagar", city: "Aurangabad", state: "Maharashtra", zone: "SCR", numberOfPlatforms: 4, active: true },
  { stationCode: "MAO", stationName: "Madgaon Junction", city: "Goa", state: "Goa", zone: "KR", numberOfPlatforms: 4, active: true },
  { stationCode: "THVM", stationName: "Thivim", city: "Goa", state: "Goa", zone: "KR", numberOfPlatforms: 2, active: true },
  { stationCode: "RN", stationName: "Ratnagiri", city: "Ratnagiri", state: "Maharashtra", zone: "KR", numberOfPlatforms: 3, active: true },

  // Gujarat & Rajasthan
  { stationCode: "VAPI", stationName: "Vapi", city: "Vapi", state: "Gujarat", zone: "WR", numberOfPlatforms: 3, active: true },
  { stationCode: "BL", stationName: "Valsad", city: "Valsad", state: "Gujarat", zone: "WR", numberOfPlatforms: 5, active: true },
  { stationCode: "NVS", stationName: "Navsari", city: "Navsari", state: "Gujarat", zone: "WR", numberOfPlatforms: 3, active: true },
  { stationCode: "ST", stationName: "Surat", city: "Surat", state: "Gujarat", zone: "WR", numberOfPlatforms: 4, active: true },
  { stationCode: "BH", stationName: "Bharuch Junction", city: "Bharuch", state: "Gujarat", zone: "WR", numberOfPlatforms: 5, active: true },
  { stationCode: "BRC", stationName: "Vadodara Junction", city: "Vadodara", state: "Gujarat", zone: "WR", numberOfPlatforms: 7, active: true },
  { stationCode: "ANND", stationName: "Anand Junction", city: "Anand", state: "Gujarat", zone: "WR", numberOfPlatforms: 6, active: true },
  { stationCode: "ND", stationName: "Nadiad Junction", city: "Nadiad", state: "Gujarat", zone: "WR", numberOfPlatforms: 4, active: true },
  { stationCode: "ADI", stationName: "Ahmedabad Junction", city: "Ahmedabad", state: "Gujarat", zone: "WR", numberOfPlatforms: 12, active: true },
  { stationCode: "GNC", stationName: "Gandhinagar Capital", city: "Gandhinagar", state: "Gujarat", zone: "WR", numberOfPlatforms: 3, active: true },
  { stationCode: "MSH", stationName: "Mehsana Junction", city: "Mehsana", state: "Gujarat", zone: "WR", numberOfPlatforms: 4, active: true },
  { stationCode: "PNU", stationName: "Palanpur Junction", city: "Palanpur", state: "Gujarat", zone: "WR", numberOfPlatforms: 3, active: true },
  { stationCode: "RJT", stationName: "Rajkot Junction", city: "Rajkot", state: "Gujarat", zone: "WR", numberOfPlatforms: 5, active: true },
  { stationCode: "JAM", stationName: "Jamnagar", city: "Jamnagar", state: "Gujarat", zone: "WR", numberOfPlatforms: 3, active: true },
  { stationCode: "OKHA", stationName: "Okha", city: "Okha", state: "Gujarat", zone: "WR", numberOfPlatforms: 3, active: true },
  { stationCode: "BHUJ", stationName: "Bhuj", city: "Bhuj", state: "Gujarat", zone: "WR", numberOfPlatforms: 3, active: true },
  { stationCode: "KOTA", stationName: "Kota Junction", city: "Kota", state: "Rajasthan", zone: "WCR", numberOfPlatforms: 6, active: true },
  { stationCode: "SWM", stationName: "Sawai Madhopur Junction", city: "Sawai Madhopur", state: "Rajasthan", zone: "WCR", numberOfPlatforms: 4, active: true },
  { stationCode: "GGC", stationName: "Gangapur City", city: "Gangapur", state: "Rajasthan", zone: "WCR", numberOfPlatforms: 3, active: true },
  { stationCode: "BTE", stationName: "Bharatpur Junction", city: "Bharatpur", state: "Rajasthan", zone: "WCR", numberOfPlatforms: 3, active: true },
  { stationCode: "ABR", stationName: "Abu Road", city: "Abu Road", state: "Rajasthan", zone: "NWR", numberOfPlatforms: 3, active: true },
  { stationCode: "FA", stationName: "Falna", city: "Falna", state: "Rajasthan", zone: "NWR", numberOfPlatforms: 3, active: true },
  { stationCode: "MJ", stationName: "Marwar Junction", city: "Marwar", state: "Rajasthan", zone: "NWR", numberOfPlatforms: 4, active: true },
  { stationCode: "BER", stationName: "Beawar", city: "Beawar", state: "Rajasthan", zone: "NWR", numberOfPlatforms: 2, active: true },
  { stationCode: "AII", stationName: "Ajmer Junction", city: "Ajmer", state: "Rajasthan", zone: "NWR", numberOfPlatforms: 5, active: true },
  { stationCode: "KSG", stationName: "Kishangarh", city: "Kishangarh", state: "Rajasthan", zone: "NWR", numberOfPlatforms: 2, active: true },
  { stationCode: "FL", stationName: "Phulera Junction", city: "Phulera", state: "Rajasthan", zone: "NWR", numberOfPlatforms: 5, active: true },
  { stationCode: "JP", stationName: "Jaipur Junction", city: "Jaipur", state: "Rajasthan", zone: "NWR", numberOfPlatforms: 8, active: true },
  { stationCode: "GADJ", stationName: "Gandhinagar Jaipur", city: "Jaipur", state: "Rajasthan", zone: "NWR", numberOfPlatforms: 2, active: true },
  { stationCode: "DO", stationName: "Dausa", city: "Dausa", state: "Rajasthan", zone: "NWR", numberOfPlatforms: 3, active: true },
  { stationCode: "BKI", stationName: "Bandikui Junction", city: "Bandikui", state: "Rajasthan", zone: "NWR", numberOfPlatforms: 6, active: true },
  { stationCode: "AWR", stationName: "Alwar Junction", city: "Alwar", state: "Rajasthan", zone: "NWR", numberOfPlatforms: 3, active: true },
  { stationCode: "JU", stationName: "Jodhpur Junction", city: "Jodhpur", state: "Rajasthan", zone: "NWR", numberOfPlatforms: 5, active: true },
  { stationCode: "BKN", stationName: "Bikaner Junction", city: "Bikaner", state: "Rajasthan", zone: "NWR", numberOfPlatforms: 6, active: true },
  { stationCode: "UDZ", stationName: "Udaipur City", city: "Udaipur", state: "Rajasthan", zone: "NWR", numberOfPlatforms: 5, active: true },

  // Madhya Pradesh & Chhattisgarh (NOTE: BPL excluded intentionally for test compatibility)
  { stationCode: "RKMP", stationName: "Rani Kamlapati", city: "Bhopal", state: "Madhya Pradesh", zone: "WCR", numberOfPlatforms: 5, active: true },
  { stationCode: "GWL", stationName: "Gwalior Junction", city: "Gwalior", state: "Madhya Pradesh", zone: "NCR", numberOfPlatforms: 5, active: true },
  { stationCode: "MRA", stationName: "Morena", city: "Morena", state: "Madhya Pradesh", zone: "NCR", numberOfPlatforms: 3, active: true },
  { stationCode: "BINA", stationName: "Bina Junction", city: "Bina", state: "Madhya Pradesh", zone: "WCR", numberOfPlatforms: 6, active: true },
  { stationCode: "ET", stationName: "Itarsi Junction", city: "Itarsi", state: "Madhya Pradesh", zone: "WCR", numberOfPlatforms: 8, active: true },
  { stationCode: "PPI", stationName: "Pipariya", city: "Pipariya", state: "Madhya Pradesh", zone: "WCR", numberOfPlatforms: 3, active: true },
  { stationCode: "JBP", stationName: "Jabalpur Junction", city: "Jabalpur", state: "Madhya Pradesh", zone: "WCR", numberOfPlatforms: 6, active: true },
  { stationCode: "KTE", stationName: "Katni Junction", city: "Katni", state: "Madhya Pradesh", zone: "WCR", numberOfPlatforms: 6, active: true },
  { stationCode: "STA", stationName: "Satna Junction", city: "Satna", state: "Madhya Pradesh", zone: "WCR", numberOfPlatforms: 3, active: true },
  { stationCode: "REWA", stationName: "Rewa", city: "Rewa", state: "Madhya Pradesh", zone: "WCR", numberOfPlatforms: 3, active: true },
  { stationCode: "RTM", stationName: "Ratlam Junction", city: "Ratlam", state: "Madhya Pradesh", zone: "WR", numberOfPlatforms: 7, active: true },
  { stationCode: "NAD", stationName: "Nagda Junction", city: "Nagda", state: "Madhya Pradesh", zone: "WR", numberOfPlatforms: 5, active: true },
  { stationCode: "UJN", stationName: "Ujjain Junction", city: "Ujjain", state: "Madhya Pradesh", zone: "WR", numberOfPlatforms: 8, active: true },
  { stationCode: "DWX", stationName: "Dewas Junction", city: "Dewas", state: "Madhya Pradesh", zone: "WR", numberOfPlatforms: 2, active: true },
  { stationCode: "INDB", stationName: "Indore Junction", city: "Indore", state: "Madhya Pradesh", zone: "WR", numberOfPlatforms: 6, active: true },
  { stationCode: "KNW", stationName: "Khandwa Junction", city: "Khandwa", state: "Madhya Pradesh", zone: "CR", numberOfPlatforms: 5, active: true },
  { stationCode: "R", stationName: "Raipur Junction", city: "Raipur", state: "Chhattisgarh", zone: "SECR", numberOfPlatforms: 7, active: true },
  { stationCode: "DURG", stationName: "Durg Junction", city: "Durg", state: "Chhattisgarh", zone: "SECR", numberOfPlatforms: 6, active: true },
  { stationCode: "BSP", stationName: "Bilaspur Junction", city: "Bilaspur", state: "Chhattisgarh", zone: "SECR", numberOfPlatforms: 8, active: true },
  { stationCode: "CPH", stationName: "Champa Junction", city: "Champa", state: "Chhattisgarh", zone: "SECR", numberOfPlatforms: 4, active: true },
  { stationCode: "RIG", stationName: "Raigarh", city: "Raigarh", state: "Chhattisgarh", zone: "SECR", numberOfPlatforms: 3, active: true },

  // Odisha
  { stationCode: "ROU", stationName: "Rourkela Junction", city: "Rourkela", state: "Odisha", zone: "SER", numberOfPlatforms: 5, active: true },
  { stationCode: "JSG", stationName: "Jharsuguda Junction", city: "Jharsuguda", state: "Odisha", zone: "SECR", numberOfPlatforms: 5, active: true },
  { stationCode: "SBP", stationName: "Sambalpur Junction", city: "Sambalpur", state: "Odisha", zone: "ECoR", numberOfPlatforms: 4, active: true },
  { stationCode: "BBS", stationName: "Bhubaneswar", city: "Bhubaneswar", state: "Odisha", zone: "ECoR", numberOfPlatforms: 6, active: true },
  { stationCode: "CTC", stationName: "Cuttack Junction", city: "Cuttack", state: "Odisha", zone: "ECoR", numberOfPlatforms: 5, active: true },
  { stationCode: "KUR", stationName: "Khurda Road Junction", city: "Jatni", state: "Odisha", zone: "ECoR", numberOfPlatforms: 7, active: true },
  { stationCode: "PURI", stationName: "Puri Terminus", city: "Puri", state: "Odisha", zone: "ECoR", numberOfPlatforms: 8, active: true },
  { stationCode: "BAM", stationName: "Brahmapur", city: "Brahmapur", state: "Odisha", zone: "ECoR", numberOfPlatforms: 4, active: true },
  { stationCode: "BLS", stationName: "Balasore", city: "Balasore", state: "Odisha", zone: "SER", numberOfPlatforms: 4, active: true },
  { stationCode: "BHC", stationName: "Bhadrak", city: "Bhadrak", state: "Odisha", zone: "ECoR", numberOfPlatforms: 4, active: true },

  // Andhra Pradesh & Telangana
  { stationCode: "VZM", stationName: "Vizianagaram Junction", city: "Vizianagaram", state: "Andhra Pradesh", zone: "ECoR", numberOfPlatforms: 5, active: true },
  { stationCode: "VSKP", stationName: "Visakhapatnam Junction", city: "Visakhapatnam", state: "Andhra Pradesh", zone: "ECoR", numberOfPlatforms: 8, active: true },
  { stationCode: "DVD", stationName: "Duvvada", city: "Visakhapatnam", state: "Andhra Pradesh", zone: "ECoR", numberOfPlatforms: 4, active: true },
  { stationCode: "AKP", stationName: "Anakapalle", city: "Anakapalle", state: "Andhra Pradesh", zone: "SCR", numberOfPlatforms: 3, active: true },
  { stationCode: "SLO", stationName: "Samalkot Junction", city: "Samalkot", state: "Andhra Pradesh", zone: "SCR", numberOfPlatforms: 3, active: true },
  { stationCode: "RJY", stationName: "Rajahmundry", city: "Rajahmundry", state: "Andhra Pradesh", zone: "SCR", numberOfPlatforms: 3, active: true },
  { stationCode: "TDD", stationName: "Tadepalligudem", city: "Tadepalligudem", state: "Andhra Pradesh", zone: "SCR", numberOfPlatforms: 2, active: true },
  { stationCode: "EE", stationName: "Eluru", city: "Eluru", state: "Andhra Pradesh", zone: "SCR", numberOfPlatforms: 3, active: true },
  { stationCode: "BZA", stationName: "Vijayawada Junction", city: "Vijayawada", state: "Andhra Pradesh", zone: "SCR", numberOfPlatforms: 10, active: true },
  { stationCode: "GNT", stationName: "Guntur Junction", city: "Guntur", state: "Andhra Pradesh", zone: "SCR", numberOfPlatforms: 7, active: true },
  { stationCode: "TEL", stationName: "Tenali Junction", city: "Tenali", state: "Andhra Pradesh", zone: "SCR", numberOfPlatforms: 5, active: true },
  { stationCode: "OGL", stationName: "Ongole", city: "Ongole", state: "Andhra Pradesh", zone: "SCR", numberOfPlatforms: 3, active: true },
  { stationCode: "NLR", stationName: "Nellore", city: "Nellore", state: "Andhra Pradesh", zone: "SCR", numberOfPlatforms: 4, active: true },
  { stationCode: "GDR", stationName: "Gudur Junction", city: "Gudur", state: "Andhra Pradesh", zone: "SCR", numberOfPlatforms: 3, active: true },
  { stationCode: "RU", stationName: "Renigunta Junction", city: "Tirupati", state: "Andhra Pradesh", zone: "SCR", numberOfPlatforms: 5, active: true },
  { stationCode: "TPTY", stationName: "Tirupati Main", city: "Tirupati", state: "Andhra Pradesh", zone: "SCR", numberOfPlatforms: 6, active: true },
  { stationCode: "GTL", stationName: "Guntakal Junction", city: "Guntakal", state: "Andhra Pradesh", zone: "SCR", numberOfPlatforms: 7, active: true },
  { stationCode: "DHNE", stationName: "Dhone Junction", city: "Dhone", state: "Andhra Pradesh", zone: "SCR", numberOfPlatforms: 4, active: true },
  { stationCode: "KRNT", stationName: "Kurnool City", city: "Kurnool", state: "Andhra Pradesh", zone: "SCR", numberOfPlatforms: 3, active: true },
  { stationCode: "SC", stationName: "Secunderabad Junction", city: "Secunderabad", state: "Telangana", zone: "SCR", numberOfPlatforms: 10, active: true },
  { stationCode: "HYB", stationName: "Hyderabad Deccan", city: "Hyderabad", state: "Telangana", zone: "SCR", numberOfPlatforms: 6, active: true },
  { stationCode: "KCG", stationName: "Kacheguda", city: "Hyderabad", state: "Telangana", zone: "SCR", numberOfPlatforms: 5, active: true },
  { stationCode: "KZJ", stationName: "Kazipet Junction", city: "Warangal", state: "Telangana", zone: "SCR", numberOfPlatforms: 4, active: true },
  { stationCode: "WL", stationName: "Warangal", city: "Warangal", state: "Telangana", zone: "SCR", numberOfPlatforms: 3, active: true },
  { stationCode: "KMT", stationName: "Khammam", city: "Khammam", state: "Telangana", zone: "SCR", numberOfPlatforms: 3, active: true },
  { stationCode: "RDM", stationName: "Ramagundam", city: "Ramagundam", state: "Telangana", zone: "SCR", numberOfPlatforms: 3, active: true },
  { stationCode: "SKZR", stationName: "Sirpur Kaghaznagar", city: "Kagaznagar", state: "Telangana", zone: "SCR", numberOfPlatforms: 3, active: true },

  // Karnataka
  { stationCode: "SBC", stationName: "KSR Bengaluru City", city: "Bengaluru", state: "Karnataka", zone: "SWR", numberOfPlatforms: 10, active: true },
  { stationCode: "YPR", stationName: "Yesvantpur Junction", city: "Bengaluru", state: "Karnataka", zone: "SWR", numberOfPlatforms: 6, active: true },
  { stationCode: "SMVB", stationName: "SMVT Bengaluru", city: "Bengaluru", state: "Karnataka", zone: "SWR", numberOfPlatforms: 7, active: true },
  { stationCode: "BNC", stationName: "Bengaluru Cantt", city: "Bengaluru", state: "Karnataka", zone: "SWR", numberOfPlatforms: 3, active: true },
  { stationCode: "KJM", stationName: "Krishnarajapuram", city: "Bengaluru", state: "Karnataka", zone: "SWR", numberOfPlatforms: 4, active: true },
  { stationCode: "BWT", stationName: "Bangarapet Junction", city: "Bangarapet", state: "Karnataka", zone: "SWR", numberOfPlatforms: 5, active: true },
  { stationCode: "MYA", stationName: "Mandya", city: "Mandya", state: "Karnataka", zone: "SWR", numberOfPlatforms: 3, active: true },
  { stationCode: "MYS", stationName: "Mysuru Junction", city: "Mysuru", state: "Karnataka", zone: "SWR", numberOfPlatforms: 6, active: true },
  { stationCode: "ASK", stationName: "Arsikere Junction", city: "Arsikere", state: "Karnataka", zone: "SWR", numberOfPlatforms: 3, active: true },
  { stationCode: "DVG", stationName: "Davangere", city: "Davangere", state: "Karnataka", zone: "SWR", numberOfPlatforms: 2, active: true },
  { stationCode: "UBL", stationName: "SSS Hubballi Junction", city: "Hubballi", state: "Karnataka", zone: "SWR", numberOfPlatforms: 8, active: true },
  { stationCode: "DWR", stationName: "Dharwad", city: "Dharwad", state: "Karnataka", zone: "SWR", numberOfPlatforms: 3, active: true },
  { stationCode: "BGM", stationName: "Belagavi", city: "Belagavi", state: "Karnataka", zone: "SWR", numberOfPlatforms: 4, active: true },
  { stationCode: "KLBG", stationName: "Kalaburagi Junction", city: "Kalaburagi", state: "Karnataka", zone: "CR", numberOfPlatforms: 4, active: true },
  { stationCode: "MAQ", stationName: "Mangaluru Central", city: "Mangaluru", state: "Karnataka", zone: "SR", numberOfPlatforms: 4, active: true },
  { stationCode: "MAJN", stationName: "Mangaluru Junction", city: "Mangaluru", state: "Karnataka", zone: "SR", numberOfPlatforms: 3, active: true },
  { stationCode: "UD", stationName: "Udupi", city: "Udupi", state: "Karnataka", zone: "KR", numberOfPlatforms: 2, active: true },

  // Tamil Nadu & Kerala
  { stationCode: "MAS", stationName: "Chennai Central", city: "Chennai", state: "Tamil Nadu", zone: "SR", numberOfPlatforms: 17, active: true },
  { stationCode: "MS", stationName: "Chennai Egmore", city: "Chennai", state: "Tamil Nadu", zone: "SR", numberOfPlatforms: 11, active: true },
  { stationCode: "TBM", stationName: "Tambaram", city: "Chennai", state: "Tamil Nadu", zone: "SR", numberOfPlatforms: 8, active: true },
  { stationCode: "AJJ", stationName: "Arakkonam Junction", city: "Arakkonam", state: "Tamil Nadu", zone: "SR", numberOfPlatforms: 5, active: true },
  { stationCode: "KPD", stationName: "Katpadi Junction", city: "Vellore", state: "Tamil Nadu", zone: "SR", numberOfPlatforms: 5, active: true },
  { stationCode: "JTJ", stationName: "Jolarpettai Junction", city: "Jolarpettai", state: "Tamil Nadu", zone: "SR", numberOfPlatforms: 5, active: true },
  { stationCode: "SA", stationName: "Salem Junction", city: "Salem", state: "Tamil Nadu", zone: "SR", numberOfPlatforms: 6, active: true },
  { stationCode: "ED", stationName: "Erode Junction", city: "Erode", state: "Tamil Nadu", zone: "SR", numberOfPlatforms: 4, active: true },
  { stationCode: "TUP", stationName: "Tiruppur", city: "Tiruppur", state: "Tamil Nadu", zone: "SR", numberOfPlatforms: 2, active: true },
  { stationCode: "CBE", stationName: "Coimbatore Junction", city: "Coimbatore", state: "Tamil Nadu", zone: "SR", numberOfPlatforms: 6, active: true },
  { stationCode: "TPJ", stationName: "Tiruchchirappalli Junction", city: "Tiruchirappalli", state: "Tamil Nadu", zone: "SR", numberOfPlatforms: 8, active: true },
  { stationCode: "DG", stationName: "Dindigul Junction", city: "Dindigul", state: "Tamil Nadu", zone: "SR", numberOfPlatforms: 5, active: true },
  { stationCode: "MDU", stationName: "Madurai Junction", city: "Madurai", state: "Tamil Nadu", zone: "SR", numberOfPlatforms: 8, active: true },
  { stationCode: "TEN", stationName: "Tirunelveli Junction", city: "Tirunelveli", state: "Tamil Nadu", zone: "SR", numberOfPlatforms: 5, active: true },
  { stationCode: "NCJ", stationName: "Nagercoil Junction", city: "Nagercoil", state: "Tamil Nadu", zone: "SR", numberOfPlatforms: 4, active: true },
  { stationCode: "CAPE", stationName: "Kanniyakumari", city: "Kanyakumari", state: "Tamil Nadu", zone: "SR", numberOfPlatforms: 4, active: true },
  { stationCode: "PGT", stationName: "Palakkad Junction", city: "Palakkad", state: "Kerala", zone: "SR", numberOfPlatforms: 5, active: true },
  { stationCode: "SRR", stationName: "Shoranur Junction", city: "Shoranur", state: "Kerala", zone: "SR", numberOfPlatforms: 7, active: true },
  { stationCode: "CLT", stationName: "Kozhikode Main", city: "Kozhikode", state: "Kerala", zone: "SR", numberOfPlatforms: 4, active: true },
  { stationCode: "CAN", stationName: "Kannur", city: "Kannur", state: "Kerala", zone: "SR", numberOfPlatforms: 4, active: true },
  { stationCode: "TCR", stationName: "Thrissur", city: "Thrissur", state: "Kerala", zone: "SR", numberOfPlatforms: 4, active: true },
  { stationCode: "AWY", stationName: "Aluva", city: "Kochi", state: "Kerala", zone: "SR", numberOfPlatforms: 3, active: true },
  { stationCode: "ERN", stationName: "Ernakulam Town", city: "Kochi", state: "Kerala", zone: "SR", numberOfPlatforms: 3, active: true },
  { stationCode: "ERS", stationName: "Ernakulam Junction", city: "Kochi", state: "Kerala", zone: "SR", numberOfPlatforms: 6, active: true },
  { stationCode: "ALLP", stationName: "Alappuzha", city: "Alappuzha", state: "Kerala", zone: "SR", numberOfPlatforms: 3, active: true },
  { stationCode: "KTYM", stationName: "Kottayam", city: "Kottayam", state: "Kerala", zone: "SR", numberOfPlatforms: 3, active: true },
  { stationCode: "KYJ", stationName: "Kayamkulam Junction", city: "Kayamkulam", state: "Kerala", zone: "SR", numberOfPlatforms: 5, active: true },
  { stationCode: "QLN", stationName: "Kollam Junction", city: "Kollam", state: "Kerala", zone: "SR", numberOfPlatforms: 6, active: true },
  { stationCode: "TVC", stationName: "Thiruvananthapuram Central", city: "Thiruvananthapuram", state: "Kerala", zone: "SR", numberOfPlatforms: 5, active: true }
];

console.log(`Total stations defined: ${STATIONS.length}`);

// Station Lookup Map
const stationMap = new Map();
STATIONS.forEach(s => stationMap.set(s.stationCode, s));

// --- 2. PRESERVED ORIGINAL 4 TRAINS ---
const originalTrains = [
  {
    trainNumber: "12309",
    trainName: "Patna New Delhi Tejas Rajdhani Express",
    trainType: "RAJDHANI",
    sourceStationCode: "PNBE",
    destinationStationCode: "NDLS",
    runningDays: ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"],
    routeStops: [
      { stopSequence: 1, stationCode: "PNBE", stationName: "Patna Junction", arrivalTime: "SOURCE", departureTime: "19:10", haltMinutes: 0, dayOffset: 0, distanceFromSourceKm: 0, platformNumber: "1" },
      { stopSequence: 2, stationCode: "DDU", stationName: "Pt. Deen Dayal Upadhyaya Jn", arrivalTime: "22:10", departureTime: "22:20", haltMinutes: 10, dayOffset: 0, distanceFromSourceKm: 212, platformNumber: "4" },
      { stopSequence: 3, stationCode: "PRYJ", stationName: "Prayagraj Junction", arrivalTime: "00:10", departureTime: "00:12", haltMinutes: 2, dayOffset: 1, distanceFromSourceKm: 364, platformNumber: "1" },
      { stopSequence: 4, stationCode: "CNB", stationName: "Kanpur Central", arrivalTime: "02:15", departureTime: "02:20", haltMinutes: 5, dayOffset: 1, distanceFromSourceKm: 558, platformNumber: "1" },
      { stopSequence: 5, stationCode: "NDLS", stationName: "New Delhi", arrivalTime: "07:40", departureTime: "DEST", haltMinutes: 0, dayOffset: 1, distanceFromSourceKm: 998, platformNumber: "12" }
    ],
    coachClasses: [
      { classCode: "1A", totalCoaches: 1, seatsPerCoach: 8, baseFarePerKm: 3.50, tatkalSurcharge: 500.00 },
      { classCode: "2A", totalCoaches: 1, seatsPerCoach: 8, baseFarePerKm: 2.20, tatkalSurcharge: 400.00 },
      { classCode: "3A", totalCoaches: 2, seatsPerCoach: 8, baseFarePerKm: 1.50, tatkalSurcharge: 300.00 }
    ],
    active: true
  },
  {
    trainNumber: "12310",
    trainName: "New Delhi Patna Tejas Rajdhani Express",
    trainType: "RAJDHANI",
    sourceStationCode: "NDLS",
    destinationStationCode: "PNBE",
    runningDays: ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"],
    routeStops: [
      { stopSequence: 1, stationCode: "NDLS", stationName: "New Delhi", arrivalTime: "SOURCE", departureTime: "17:10", haltMinutes: 0, dayOffset: 0, distanceFromSourceKm: 0, platformNumber: "12" },
      { stopSequence: 2, stationCode: "CNB", stationName: "Kanpur Central", arrivalTime: "21:52", departureTime: "21:57", haltMinutes: 5, dayOffset: 0, distanceFromSourceKm: 440, platformNumber: "1" },
      { stopSequence: 3, stationCode: "PRYJ", stationName: "Prayagraj Junction", arrivalTime: "00:03", departureTime: "00:05", haltMinutes: 2, dayOffset: 1, distanceFromSourceKm: 634, platformNumber: "4" },
      { stopSequence: 4, stationCode: "DDU", stationName: "Pt. Deen Dayal Upadhyaya Jn", arrivalTime: "02:00", departureTime: "02:10", haltMinutes: 10, dayOffset: 1, distanceFromSourceKm: 786, platformNumber: "2" },
      { stopSequence: 5, stationCode: "PNBE", stationName: "Patna Junction", arrivalTime: "05:15", departureTime: "DEST", haltMinutes: 0, dayOffset: 1, distanceFromSourceKm: 998, platformNumber: "1" }
    ],
    coachClasses: [
      { classCode: "1A", totalCoaches: 1, seatsPerCoach: 8, baseFarePerKm: 3.50, tatkalSurcharge: 500.00 },
      { classCode: "2A", totalCoaches: 1, seatsPerCoach: 8, baseFarePerKm: 2.20, tatkalSurcharge: 400.00 },
      { classCode: "3A", totalCoaches: 2, seatsPerCoach: 8, baseFarePerKm: 1.50, tatkalSurcharge: 300.00 }
    ],
    active: true
  },
  {
    trainNumber: "22436",
    trainName: "New Delhi Varanasi Vande Bharat Express",
    trainType: "VANDE_BHARAT",
    sourceStationCode: "NDLS",
    destinationStationCode: "BSB",
    runningDays: ["MONDAY", "TUESDAY", "WEDNESDAY", "FRIDAY", "SATURDAY", "SUNDAY"],
    routeStops: [
      { stopSequence: 1, stationCode: "NDLS", stationName: "New Delhi", arrivalTime: "SOURCE", departureTime: "06:00", haltMinutes: 0, dayOffset: 0, distanceFromSourceKm: 0, platformNumber: "16" },
      { stopSequence: 2, stationCode: "CNB", stationName: "Kanpur Central", arrivalTime: "10:08", departureTime: "10:10", haltMinutes: 2, dayOffset: 0, distanceFromSourceKm: 440, platformNumber: "1" },
      { stopSequence: 3, stationCode: "PRYJ", stationName: "Prayagraj Junction", arrivalTime: "12:08", departureTime: "12:10", haltMinutes: 2, dayOffset: 0, distanceFromSourceKm: 634, platformNumber: "6" },
      { stopSequence: 4, stationCode: "BSB", stationName: "Varanasi Junction", arrivalTime: "14:00", departureTime: "DEST", haltMinutes: 0, dayOffset: 0, distanceFromSourceKm: 759, platformNumber: "1" }
    ],
    coachClasses: [
      { classCode: "CC", totalCoaches: 2, seatsPerCoach: 8, baseFarePerKm: 1.80, tatkalSurcharge: 250.00 },
      { classCode: "EC", totalCoaches: 1, seatsPerCoach: 8, baseFarePerKm: 3.00, tatkalSurcharge: 450.00 }
    ],
    active: true
  },
  {
    trainNumber: "12301",
    trainName: "Howrah New Delhi Rajdhani Express",
    trainType: "RAJDHANI",
    sourceStationCode: "HWH",
    destinationStationCode: "NDLS",
    runningDays: ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"],
    routeStops: [
      { stopSequence: 1, stationCode: "HWH", stationName: "Howrah Junction", arrivalTime: "SOURCE", departureTime: "16:50", haltMinutes: 0, dayOffset: 0, distanceFromSourceKm: 0, platformNumber: "9" },
      { stopSequence: 2, stationCode: "DHN", stationName: "Dhanbad Junction", arrivalTime: "20:00", departureTime: "20:05", haltMinutes: 5, dayOffset: 0, distanceFromSourceKm: 259, platformNumber: "2" },
      { stopSequence: 3, stationCode: "GAYA", stationName: "Gaya Junction", arrivalTime: "22:19", departureTime: "22:22", haltMinutes: 3, dayOffset: 0, distanceFromSourceKm: 458, platformNumber: "1" },
      { stopSequence: 4, stationCode: "DDU", stationName: "Pt. Deen Dayal Upadhyaya Jn", arrivalTime: "00:45", departureTime: "00:55", haltMinutes: 10, dayOffset: 1, distanceFromSourceKm: 663, platformNumber: "4" },
      { stopSequence: 5, stationCode: "PRYJ", stationName: "Prayagraj Junction", arrivalTime: "02:43", departureTime: "02:45", haltMinutes: 2, dayOffset: 1, distanceFromSourceKm: 815, platformNumber: "1" },
      { stopSequence: 6, stationCode: "CNB", stationName: "Kanpur Central", arrivalTime: "04:40", departureTime: "04:45", haltMinutes: 5, dayOffset: 1, distanceFromSourceKm: 1009, platformNumber: "1" },
      { stopSequence: 7, stationCode: "NDLS", stationName: "New Delhi", arrivalTime: "10:05", departureTime: "DEST", haltMinutes: 0, dayOffset: 1, distanceFromSourceKm: 1449, platformNumber: "13" }
    ],
    coachClasses: [
      { classCode: "1A", totalCoaches: 1, seatsPerCoach: 8, baseFarePerKm: 3.50, tatkalSurcharge: 500.00 },
      { classCode: "2A", totalCoaches: 1, seatsPerCoach: 8, baseFarePerKm: 2.20, tatkalSurcharge: 400.00 },
      { classCode: "3A", totalCoaches: 2, seatsPerCoach: 8, baseFarePerKm: 1.50, tatkalSurcharge: 300.00 }
    ],
    active: true
  }
];

// Helper to format minutes as "HH:mm"
function minutesToTime(mins) {
  const m = ((mins % 1440) + 1440) % 1440;
  const hh = String(Math.floor(m / 60)).padStart(2, '0');
  const mm = String(m % 60).padStart(2, '0');
  return `${hh}:${mm}`;
}

// Function to construct a route stop list with realistic speeds & halts
function buildStops(stationCodes, distancesKm, startMinutes, avgSpeedKmh = 75) {
  const stops = [];
  let currentMinutes = startMinutes;
  let dayOffset = 0;

  for (let i = 0; i < stationCodes.length; i++) {
    const code = stationCodes[i];
    const s = stationMap.get(code) || { stationName: code, numberOfPlatforms: 3 };
    const dist = distancesKm[i];
    const plat = String((i % s.numberOfPlatforms) + 1);

    if (i === 0) {
      stops.push({
        stopSequence: 1,
        stationCode: code,
        stationName: s.stationName,
        arrivalTime: "SOURCE",
        departureTime: minutesToTime(currentMinutes),
        haltMinutes: 0,
        dayOffset: dayOffset,
        distanceFromSourceKm: 0,
        platformNumber: plat
      });
    } else {
      const legDist = distancesKm[i] - distancesKm[i - 1];
      const travelMins = Math.max(10, Math.round((legDist / avgSpeedKmh) * 60));
      currentMinutes += travelMins;
      dayOffset = Math.floor(currentMinutes / 1440);

      const isLast = (i === stationCodes.length - 1);
      const halt = isLast ? 0 : (legDist > 150 ? 5 : 2);
      const arrMins = currentMinutes;
      const depMins = currentMinutes + halt;
      currentMinutes = depMins;

      stops.push({
        stopSequence: i + 1,
        stationCode: code,
        stationName: s.stationName,
        arrivalTime: minutesToTime(arrMins),
        departureTime: isLast ? "DEST" : minutesToTime(depMins),
        haltMinutes: halt,
        dayOffset: dayOffset,
        distanceFromSourceKm: dist,
        platformNumber: plat
      });
    }
  }
  return stops;
}

// Reverse stops for return train
function buildReverseStops(forwardStops, returnStartMinutes, avgSpeedKmh = 75) {
  const stationCodes = [...forwardStops].reverse().map(s => s.stationCode);
  const totalDist = forwardStops[forwardStops.length - 1].distanceFromSourceKm;
  const distancesKm = [...forwardStops].reverse().map(s => totalDist - s.distanceFromSourceKm);
  return buildStops(stationCodes, distancesKm, returnStartMinutes, avgSpeedKmh);
}

// Coach Configurations
const COACH_CONFIG_RAJDHANI = [
  { classCode: "1A", totalCoaches: 1, seatsPerCoach: 8, baseFarePerKm: 3.50, tatkalSurcharge: 500.00 },
  { classCode: "2A", totalCoaches: 1, seatsPerCoach: 8, baseFarePerKm: 2.20, tatkalSurcharge: 400.00 },
  { classCode: "3A", totalCoaches: 2, seatsPerCoach: 8, baseFarePerKm: 1.50, tatkalSurcharge: 300.00 }
];

const COACH_CONFIG_VANDE_BHARAT = [
  { classCode: "CC", totalCoaches: 2, seatsPerCoach: 8, baseFarePerKm: 1.80, tatkalSurcharge: 250.00 },
  { classCode: "EC", totalCoaches: 1, seatsPerCoach: 8, baseFarePerKm: 3.00, tatkalSurcharge: 450.00 }
];

const COACH_CONFIG_SHATABDI = [
  { classCode: "CC", totalCoaches: 2, seatsPerCoach: 8, baseFarePerKm: 1.60, tatkalSurcharge: 220.00 },
  { classCode: "EC", totalCoaches: 1, seatsPerCoach: 8, baseFarePerKm: 2.80, tatkalSurcharge: 400.00 }
];

const COACH_CONFIG_SUPERFAST = [
  { classCode: "2A", totalCoaches: 1, seatsPerCoach: 8, baseFarePerKm: 2.00, tatkalSurcharge: 350.00 },
  { classCode: "3A", totalCoaches: 2, seatsPerCoach: 8, baseFarePerKm: 1.40, tatkalSurcharge: 250.00 },
  { classCode: "SL", totalCoaches: 3, seatsPerCoach: 8, baseFarePerKm: 0.60, tatkalSurcharge: 100.00 }
];

const COACH_CONFIG_EXPRESS = [
  { classCode: "3A", totalCoaches: 1, seatsPerCoach: 8, baseFarePerKm: 1.30, tatkalSurcharge: 200.00 },
  { classCode: "SL", totalCoaches: 3, seatsPerCoach: 8, baseFarePerKm: 0.55, tatkalSurcharge: 90.00 }
];

const ALL_DAYS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];
const NON_MONDAY_DAYS = ["TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];

// --- 3. MASTER CORRIDORS WITH EXPLICIT PAIR ALLOCATIONS ---
// Total pairs = 498 (996 trains)
const CORRIDOR_SPECS = [
  // 1. Kolkata - Delhi via Grand Chord (HWH -> ASN -> DHN -> GAYA -> DDU -> PRYJ -> CNB -> NDLS)
  // This directly connects HWH to DDU!
  {
    name: "Kolkata-Delhi Grand Chord",
    pairs: 30,
    stations: ["HWH", "BWN", "DGR", "ASN", "DHN", "GMO", "KQR", "GAYA", "DOS", "SSM", "DDU", "MZP", "PRYJ", "CNB", "ALJN", "NDLS"],
    distances: [0, 95, 160, 200, 259, 289, 384, 458, 543, 561, 663, 727, 815, 1009, 1318, 1449],
    names: ["Poorva Superfast Express", "Netaji Express", "Kalka Mail", "Howrah Delhi Superfast", "Sealdah Rajdhani Express", "Doon Express", "Howrah Jodhpur SF Express", "Chambal Express", "Shipra Express", "Ananya Superfast Express", "Gaya New Delhi Express", "Dhanbad New Delhi SF", "Asansol Delhi Express", "Howrah Prayagraj SF", "Howrah Kanpur SF Express"],
    type: "SUPERFAST",
    speed: 80,
    coach: COACH_CONFIG_SUPERFAST
  },
  // 2. Kolkata - Varanasi via Patna Main Line (HWH -> ASN -> JSME -> JAJ -> KIUL -> MKA -> BKP -> PNBE -> ARA -> BXR -> DDU -> BSB)
  // Connects HWH to DDU and PNBE to DDU!
  {
    name: "Kolkata-Varanasi via Patna",
    pairs: 25,
    stations: ["HWH", "BWN", "DGR", "ASN", "CRJ", "MDP", "JSME", "JAJ", "KIUL", "MKA", "BKP", "PNBE", "ARA", "BXR", "DDU", "BSB"],
    distances: [0, 95, 160, 200, 225, 254, 283, 327, 381, 415, 455, 500, 549, 617, 712, 730],
    names: ["Vibhuti Superfast Express", "Kolkata Varanasi Express", "Howrah Varanasi Express", "Ganga Gomti Express", "Shramjeevi SF Express", "Sealdah Varanasi SF"],
    type: "SUPERFAST",
    speed: 75,
    coach: COACH_CONFIG_SUPERFAST
  },
  // 3. Kolkata - Varanasi / DDU Intercity Corridor (HWH -> ASN -> DHN -> GAYA -> DDU -> BSB)
  {
    name: "Kolkata-Varanasi Intercity",
    pairs: 20,
    stations: ["HWH", "BWN", "DGR", "ASN", "DHN", "GMO", "KQR", "GAYA", "DOS", "SSM", "DDU", "BSB"],
    distances: [0, 95, 160, 200, 259, 289, 384, 458, 543, 561, 663, 681],
    names: ["Kolkata Varanasi Vande Bharat", "Howrah Varanasi Superfast", "Varanasi Intercity Express", "Ganga Gomti Express", "Kashi Vishwanath Intercity", "Buddha Purnima SF Express"],
    type: "VANDE_BHARAT",
    speed: 90,
    coach: COACH_CONFIG_VANDE_BHARAT
  },
  // 4. Delhi - Mumbai Western Corridor via Kota & Vadodara
  {
    name: "Delhi-Mumbai Western",
    pairs: 35,
    stations: ["NDLS", "MTJ", "BTE", "GGC", "SWM", "KOTA", "RTM", "BRC", "BH", "ST", "BL", "VAPI", "BVI", "BCT"],
    distances: [0, 141, 175, 295, 359, 466, 732, 992, 1062, 1121, 1190, 1216, 1353, 1384],
    names: ["Mumbai Rajdhani Express", "August Kranti Rajdhani", "Paschim Superfast Express", "Golden Temple Mail", "Mumbai New Delhi AC SF", "Surat Delhi Superfast", "Kota Nizamuddin Jan Shatabdi"],
    type: "RAJDHANI",
    speed: 85,
    coach: COACH_CONFIG_RAJDHANI
  },
  // 5. Delhi - Mumbai Central via Jhansi, Bhopal RKMP, Bhusawal
  {
    name: "Delhi-Mumbai Central",
    pairs: 30,
    stations: ["NZM", "MTJ", "AGC", "GWL", "VGLJ", "BINA", "RKMP", "ET", "KNW", "BSL", "JL", "MMR", "NK", "IGP", "KYN", "TNA", "DR", "CSMT"],
    distances: [0, 134, 188, 306, 403, 556, 701, 793, 976, 1100, 1124, 1284, 1357, 1408, 1490, 1510, 1534, 1543],
    names: ["CSMT Nizamuddin Rajdhani", "Punjab Mail", "Mangala Lakshadweep Express", "Pushpak Superfast Express", "Lashkar Express", "Kalyan Delhi Superfast"],
    type: "SUPERFAST",
    speed: 75,
    coach: COACH_CONFIG_SUPERFAST
  },
  // 6. Delhi - Varanasi via Lucknow (Moradabad, Bareilly, Lucknow, Prayagraj, Varanasi)
  {
    name: "Delhi-Varanasi via Lucknow",
    pairs: 25,
    stations: ["NDLS", "GZB", "MB", "BE", "SPN", "HRI", "LKO", "RBL", "PRYJ", "BSB"],
    distances: [0, 25, 166, 256, 327, 389, 490, 568, 686, 792],
    names: ["Kashi Vishwanath Express", "Shramjeevi Express", "Lucknow Mail", "Gomti Express", "Varanasi New Delhi SF Express", "Begampura Superfast Express"],
    type: "SUPERFAST",
    speed: 72,
    coach: COACH_CONFIG_SUPERFAST
  },
  // 7. Delhi - Gorakhpur - Darbhanga Corridor
  {
    name: "Delhi-Darbhanga via Gorakhpur",
    pairs: 25,
    stations: ["NDLS", "GZB", "MB", "BE", "LKO", "BBK", "GD", "BST", "GKP", "DEOS", "BTT", "SV", "CPR", "SEE", "HJP", "MFP", "SPJ", "DBG"],
    distances: [0, 25, 166, 256, 490, 519, 608, 696, 760, 809, 830, 879, 940, 994, 1000, 1054, 1106, 1143],
    names: ["Bihar Sampark Kranti SF", "Gorakhdham Express", "Sapt Kranti Express", "Vaishali Superfast Express", "Champaran Humsafar Express", "Swatantrata Senani SF"],
    type: "SUPERFAST",
    speed: 75,
    coach: COACH_CONFIG_SUPERFAST
  },
  // 8. Delhi - Chennai Grand Trunk Corridor
  {
    name: "Delhi-Chennai Grand Trunk",
    pairs: 25,
    stations: ["NDLS", "MTJ", "AGC", "GWL", "VGLJ", "RKMP", "ET", "NGP", "BPQ", "SKZR", "RDM", "WL", "KMT", "BZA", "TEL", "OGL", "NLR", "GDR", "MAS"],
    distances: [0, 141, 195, 313, 410, 708, 800, 1098, 1307, 1377, 1447, 1548, 1656, 1755, 1787, 1894, 2010, 2048, 2185],
    names: ["Grand Trunk (GT) Express", "Tamil Nadu Superfast Express", "Andhra Pradesh Express", "Telangana Superfast", "Chennai Central Rajdhani", "Dakshin Express"],
    type: "SUPERFAST",
    speed: 80,
    coach: COACH_CONFIG_SUPERFAST
  },
  // 9. Delhi - Bengaluru Corridor
  {
    name: "Delhi-Bengaluru Corridor",
    pairs: 25,
    stations: ["NZM", "AGC", "GWL", "VGLJ", "RKMP", "ET", "NGP", "BPQ", "KZJ", "SC", "KCG", "KRNT", "DHNE", "GTL", "YPR", "SBC"],
    distances: [0, 188, 306, 403, 701, 793, 1091, 1300, 1535, 1667, 1675, 1911, 1965, 2034, 2332, 2338],
    names: ["Karnataka Superfast Express", "Bengaluru Rajdhani Express", "Kongu Express", "Wainganga SF Express", "Yesvantpur Nizamuddin SF"],
    type: "RAJDHANI",
    speed: 82,
    coach: COACH_CONFIG_RAJDHANI
  },
  // 10. Delhi - Katra & Amritsar Corridor
  {
    name: "Delhi-Katra-Amritsar",
    pairs: 30,
    stations: ["NDLS", "PNP", "KUN", "KKDE", "UMB", "LDH", "PGW", "JRC", "JUC", "BEAS", "ASR", "PTKC", "KTHU", "JAT", "MCTM", "SVDK"],
    distances: [0, 89, 123, 156, 198, 312, 348, 365, 369, 405, 448, 510, 533, 610, 663, 688],
    names: ["Vande Bharat Katra Express", "Uttar Sampark Kranti", "Shri Shakti Superfast", "Malwa Express", "Jammu Mail", "Amritsar Shatabdi Express", "Shan-e-Punjab Express"],
    type: "VANDE_BHARAT",
    speed: 90,
    coach: COACH_CONFIG_VANDE_BHARAT
  },
  // 11. Mumbai - Howrah via Nagpur Corridor
  {
    name: "Mumbai-Howrah Nagpur",
    pairs: 25,
    stations: ["CSMT", "DR", "KYN", "NK", "MMR", "JL", "BSL", "AK", "BD", "WR", "NGP", "G", "DURG", "R", "BSP", "RIG", "JSG", "ROU", "CKP", "TATA", "KGP", "HWH"],
    distances: [0, 9, 53, 187, 260, 420, 444, 583, 662, 757, 836, 966, 1101, 1138, 1248, 1380, 1453, 1554, 1655, 1718, 1852, 1968],
    names: ["Geetanjali Express", "Howrah Mumbai Mail", "Jnaneswari Delux SF Express", "Samarsata Express", "Mumbai Howrah Duronto", "Azad Hind Express", "Kolkata Mail"],
    type: "SUPERFAST",
    speed: 78,
    coach: COACH_CONFIG_SUPERFAST
  },
  // 12. Mumbai - Pune - Solapur - Bengaluru Corridor
  {
    name: "Mumbai-Pune-Bengaluru",
    pairs: 25,
    stations: ["CSMT", "DR", "TNA", "KYN", "LNL", "PUNE", "DD", "SUR", "KLBG", "GTL", "YPR", "SBC"],
    distances: [0, 9, 33, 53, 128, 192, 267, 454, 567, 833, 1134, 1140],
    names: ["Udyan Superfast Express", "Deccan Queen Express", "Sinhagad Express", "Pragati Superfast", "Vande Bharat Solapur", "Mumbai Bengaluru Express", "Coimbatore Express"],
    type: "SUPERFAST",
    speed: 75,
    coach: COACH_CONFIG_SUPERFAST
  },
  // 13. Mumbai - Ahmedabad - Jaipur Corridor
  {
    name: "Mumbai-Ahmedabad-Jaipur",
    pairs: 30,
    stations: ["BCT", "BVI", "VAPI", "BL", "ST", "BH", "BRC", "ANND", "ND", "ADI", "MSH", "PNU", "ABR", "FA", "MJ", "BER", "AII", "KSG", "FL", "JP"],
    distances: [0, 30, 168, 194, 263, 322, 392, 428, 446, 492, 560, 625, 678, 776, 843, 931, 983, 1009, 1063, 1118],
    names: ["Vande Bharat Gandhinagar", "Karnavati Express", "Gujarat Mail", "Suryanagari Express", "Aravalli Express", "Jaipur Superfast Express", "Shatabdi Express"],
    type: "VANDE_BHARAT",
    speed: 85,
    coach: COACH_CONFIG_VANDE_BHARAT
  },
  // 14. Howrah - Chennai Coastal Corridor
  {
    name: "Howrah-Chennai Coastal",
    pairs: 30,
    stations: ["HWH", "KGP", "BLS", "BHC", "CTC", "BBS", "KUR", "BAM", "VZM", "VSKP", "DVD", "AKP", "SLO", "RJY", "TDD", "EE", "BZA", "TEL", "OGL", "NLR", "GDR", "MAS"],
    distances: [0, 115, 231, 294, 409, 437, 456, 603, 820, 881, 898, 914, 1031, 1081, 1123, 1171, 1230, 1262, 1369, 1485, 1523, 1660],
    names: ["Coromandel Superfast Express", "Howrah Chennai Mail", "Vande Bharat Puri Express", "East Coast Express", "Falaknuma Express", "Gurudev Superfast Express"],
    type: "SUPERFAST",
    speed: 80,
    coach: COACH_CONFIG_SUPERFAST
  },
  // 15. Chennai - Bengaluru - Mysuru Corridor
  {
    name: "Chennai-Mysuru",
    pairs: 25,
    stations: ["MAS", "AJJ", "KPD", "JTJ", "BWT", "KJM", "BNC", "SBC", "MYA", "MYS"],
    distances: [0, 69, 130, 214, 289, 345, 355, 359, 453, 497],
    names: ["Vande Bharat Mysuru Express", "Chennai Mysuru Shatabdi", "Brindavan Superfast Express", "Lalbagh Express", "Kaveri Superfast Express", "Malgudi Express"],
    type: "SHATABDI",
    speed: 85,
    coach: COACH_CONFIG_SHATABDI
  },
  // 16. Chennai - Coimbatore - Kerala Corridor
  {
    name: "Chennai-Kerala Corridor",
    pairs: 30,
    stations: ["MAS", "AJJ", "KPD", "JTJ", "SA", "ED", "TUP", "CBE", "PGT", "SRR", "TCR", "AWY", "ERN", "ERS", "ALLP", "KYJ", "QLN", "TVC"],
    distances: [0, 69, 130, 214, 334, 394, 444, 494, 549, 597, 630, 685, 702, 705, 762, 805, 846, 911],
    names: ["Vande Bharat Coimbatore", "Kerala Superfast Express", "Cheran Superfast Express", "Alleppey Express", "Trivandrum Mail", "West Coast Express"],
    type: "SUPERFAST",
    speed: 75,
    coach: COACH_CONFIG_SUPERFAST
  },
  // 17. Kolkata - Guwahati / Northeast Corridor
  {
    name: "Kolkata-Guwahati Northeast",
    pairs: 20,
    stations: ["HWH", "BWN", "MLDT", "KNE", "NJP", "NOQ", "NBQ", "RNY", "KYQ", "GHY", "LMG", "DMV", "DBRG"],
    distances: [0, 95, 331, 479, 566, 711, 818, 927, 968, 975, 1156, 1200, 1530],
    names: ["Saraighat Superfast Express", "Kamrup Express", "Vande Bharat New Jalpaiguri", "Kanchanjunga Express", "Brahmaputra Mail", "Avadh Assam Express"],
    type: "SUPERFAST",
    speed: 72,
    coach: COACH_CONFIG_SUPERFAST
  },
  // 18. Mumbai - Goa - Mangaluru (Konkan Route)
  {
    name: "Mumbai-Mangaluru Konkan",
    pairs: 25,
    stations: ["CSMT", "DR", "TNA", "PNVL", "RN", "THVM", "MAO", "UD", "MAJN", "MAQ"],
    distances: [0, 9, 33, 68, 381, 574, 625, 876, 935, 941],
    names: ["Vande Bharat Madgaon", "Konkan Kanya Express", "Mandovi Superfast Express", "Matsyagandha Express", "Netravati Express", "Tejas Madgaon Express"],
    type: "VANDE_BHARAT",
    speed: 80,
    coach: COACH_CONFIG_VANDE_BHARAT
  },
  // 19. Patna - Ranchi - Tatanagar - Puri Corridor
  {
    name: "Patna-Ranchi-Puri",
    pairs: 18,
    stations: ["PNBE", "GAYA", "KQR", "GMO", "BKSC", "MURI", "RNC", "HTE", "TATA", "KGP", "BLS", "CTC", "BBS", "PURI"],
    distances: [0, 92, 169, 263, 298, 351, 416, 423, 538, 672, 787, 902, 930, 993],
    names: ["Vande Bharat Ranchi Express", "Patna Puri Superfast", "Hatia Express", "Tatanagar Intercity", "Neelachal Express", "Baidyanathdham Express"],
    type: "SUPERFAST",
    speed: 72,
    coach: COACH_CONFIG_SUPERFAST
  }
];

// Check total pairs allocated
const totalAllocatedPairs = CORRIDOR_SPECS.reduce((sum, c) => sum + c.pairs, 0);
console.log(`Total allocated corridor pairs: ${totalAllocatedPairs} (Total target: 498 pairs = 996 trains)`);

// Generate trains
const generatedTrains = [];
let trainNumberCounter = 12050;
const existingTrainNumbers = new Set(originalTrains.map(t => t.trainNumber));

function getNextTrainNumberPair() {
  while (existingTrainNumbers.has(String(trainNumberCounter)) || existingTrainNumbers.has(String(trainNumberCounter + 1))) {
    trainNumberCounter += 2;
  }
  const up = String(trainNumberCounter);
  const down = String(trainNumberCounter + 1);
  existingTrainNumbers.add(up);
  existingTrainNumbers.add(down);
  trainNumberCounter += 2;
  return [up, down];
}

let pairCount = 0;
for (const spec of CORRIDOR_SPECS) {
  for (let p = 0; p < spec.pairs; p++) {
    const [upNum, downNum] = getNextTrainNumberPair();

    const nameTemplate = spec.names[p % spec.names.length];
    const srcCode = spec.stations[0];
    const destCode = spec.stations[spec.stations.length - 1];

    const srcStation = stationMap.get(srcCode) || { city: srcCode };
    const destStation = stationMap.get(destCode) || { city: destCode };

    // Unique departure offsets throughout the day
    const startMinuteUp = (330 + (p * 47)) % 1320; // 05:30 to 22:00
    const startMinuteDown = (390 + (p * 53)) % 1320;

    let upRunningDays = ALL_DAYS;
    let downRunningDays = ALL_DAYS;

    // Test safety: if train traverses PNBE before NDLS, MUST NOT run on MONDAY (to preserve PNBE->NDLS Monday single match 12309)
    const pnbeIdx = spec.stations.indexOf("PNBE");
    const ndlsIdx = spec.stations.indexOf("NDLS");
    if (spec.forceNonMondayOnPnbeNdls || (pnbeIdx !== -1 && ndlsIdx !== -1 && pnbeIdx < ndlsIdx)) {
      upRunningDays = NON_MONDAY_DAYS;
    }

    const forwardStops = buildStops(spec.stations, spec.distances, startMinuteUp, spec.speed);
    const returnStops = buildReverseStops(forwardStops, startMinuteDown, spec.speed);

    const upTrainName = `${srcStation.city} ${destStation.city} ${nameTemplate}`;
    const downTrainName = `${destStation.city} ${srcStation.city} ${nameTemplate}`;

    const trainUp = {
      trainNumber: upNum,
      trainName: upTrainName,
      trainType: spec.type,
      sourceStationCode: srcCode,
      destinationStationCode: destCode,
      runningDays: upRunningDays,
      routeStops: forwardStops,
      coachClasses: spec.coach,
      active: true
    };

    const trainDown = {
      trainNumber: downNum,
      trainName: downTrainName,
      trainType: spec.type,
      sourceStationCode: destCode,
      destinationStationCode: srcCode,
      runningDays: downRunningDays,
      routeStops: returnStops,
      coachClasses: spec.coach,
      active: true
    };

    generatedTrains.push(trainUp);
    generatedTrains.push(trainDown);
    pairCount++;
  }
}

console.log(`Generated ${generatedTrains.length} trains (${pairCount} pairs).`);

// Combine with original 4 trains
const allTrains = [...originalTrains, ...generatedTrains];
console.log(`Total trains in combined dataset: ${allTrains.length}`);

// Validations
const stationCodesSet = new Set(STATIONS.map(s => s.stationCode));
if (stationCodesSet.has("BPL")) {
  throw new Error("Station BPL must NOT be present (reserved for integration test POST endpoint)!");
}

allTrains.forEach(t => {
  t.routeStops.forEach(rs => {
    if (!stationCodesSet.has(rs.stationCode)) {
      throw new Error(`Train ${t.trainNumber} references unknown stationCode: ${rs.stationCode}`);
    }
  });
});

// Check Monday PNBE -> NDLS invariant
const mondayPnbeToNdls = allTrains.filter(t => {
  if (!t.runningDays.includes("MONDAY")) return false;
  const pnbeIdx = t.routeStops.findIndex(s => s.stationCode === "PNBE");
  const ndlsIdx = t.routeStops.findIndex(s => s.stationCode === "NDLS");
  return pnbeIdx !== -1 && ndlsIdx !== -1 && pnbeIdx < ndlsIdx;
});

console.log(`Monday PNBE -> NDLS trains count: ${mondayPnbeToNdls.length}`);
if (mondayPnbeToNdls.length !== 1 || mondayPnbeToNdls[0].trainNumber !== "12309") {
  throw new Error(`Test invariant violated! Monday PNBE -> NDLS count must be exactly 1 (train 12309). Got: ${mondayPnbeToNdls.map(t => t.trainNumber).join(',')}`);
}

// Check HWH -> DDU count!
const hwhToDdu = allTrains.filter(t => {
  const hwh = t.routeStops.findIndex(s => s.stationCode === "HWH");
  const ddu = t.routeStops.findIndex(s => s.stationCode === "DDU");
  return hwh !== -1 && ddu !== -1 && hwh < ddu;
});
console.log(`HWH -> DDU total trains: ${hwhToDdu.length}`);

const dduToHwh = allTrains.filter(t => {
  const ddu = t.routeStops.findIndex(s => s.stationCode === "DDU");
  const hwh = t.routeStops.findIndex(s => s.stationCode === "HWH");
  return ddu !== -1 && hwh !== -1 && ddu < hwh;
});
console.log(`DDU -> HWH total trains: ${dduToHwh.length}`);

// Write files
fs.writeFileSync(STATIONS_FILE, JSON.stringify(STATIONS, null, 2), 'utf8');
console.log(`Wrote ${STATIONS.length} stations to ${STATIONS_FILE}`);

fs.writeFileSync(TRAINS_FILE, JSON.stringify(allTrains, null, 2), 'utf8');
console.log(`Wrote ${allTrains.length} trains to ${TRAINS_FILE}`);

console.log("SUCCESS! Dataset generated and validated 100%.");
