# 🚆 Indian Railways Trains & Stations Dataset

A clean, production-grade JSON dataset of **302 Indian Railway Stations** and **1,000 Popular Indian Trains** with complete route timetables, intermediate stoppage sequences, day offsets, distances, running days, and travel classes.

This dataset was created for the **RailConnect** high-throughput ticketing engine, and is open-sourced here for developers, data scientists, researchers, and open-source contributors building railway apps, graph algorithms, routing systems, or search engines.

---

## 📁 Files Included

| File | Size | Records | Description |
|---|---|---|---|
| [`stations.json`](./stations.json) | ~56 KB | **302 Stations** | Indian Railway stations across all zones with code, name, city, state, zone & platforms. |
| [`trains.json`](./trains.json) | ~5.5 MB | **1,000 Trains** | Full timetables & routes with intermediate stoppages, arrival/departure, day offset & distance. |
| [`generate_dataset.js`](./generate_dataset.js) | ~72 KB | Node.js Script | Standalone generator script used to synthesize and validate the dataset. |

---

## 🚉 Stations Dataset (`stations.json`)

### Schema:
```json
{
  "stationCode": "NDLS",
  "stationName": "New Delhi",
  "city": "New Delhi",
  "state": "Delhi",
  "zone": "NR",
  "numberOfPlatforms": 16,
  "active": true
}
```

### Coverage Highlights:
- **Major Hubs**: New Delhi (`NDLS`), Howrah (`HWH`), Mumbai Central (`BCT`), CSMT (`CSMT`), Chennai Central (`MAS`), Bengaluru City (`SBC`), Patna Junction (`PNBE`), Pt. Deen Dayal Upadhyaya (`DDU`), Prayagraj (`PRYJ`), Kanpur Central (`CNB`), Ahmedabad (`ADI`), etc.
- **Railway Zones**: Covers all Indian Railway zones — **NR**, **NCR**, **ER**, **ECR**, **WR**, **WCR**, **CR**, **SR**, **SCR**, **SWR**, **SECR**, **NER**, **NFR**, **NWR**, **ECoR**.

---

## 🚅 Trains Dataset (`trains.json`)

### Schema:
```json
{
  "trainNumber": "12309",
  "trainName": "Patna New Delhi Tejas Rajdhani Express",
  "sourceStationCode": "PNBE",
  "destinationStationCode": "NDLS",
  "runningDays": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"],
  "availableClasses": ["1A", "2A", "3A", "3E"],
  "active": true,
  "stoppages": [
    {
      "stationCode": "PNBE",
      "stopSequence": 1,
      "arrivalTime": "19:10",
      "departureTime": "19:10",
      "distanceFromSourceKm": 0,
      "dayOffset": 0
    },
    {
      "stationCode": "DDU",
      "stopSequence": 2,
      "arrivalTime": "22:12",
      "departureTime": "22:22",
      "distanceFromSourceKm": 211,
      "dayOffset": 0
    },
    {
      "stationCode": "PRYJ",
      "stopSequence": 3,
      "arrivalTime": "00:10",
      "departureTime": "00:12",
      "distanceFromSourceKm": 364,
      "dayOffset": 1
    },
    {
      "stationCode": "CNB",
      "stopSequence": 4,
      "arrivalTime": "02:15",
      "departureTime": "02:20",
      "distanceFromSourceKm": 559,
      "dayOffset": 1
    },
    {
      "stationCode": "NDLS",
      "stopSequence": 5,
      "arrivalTime": "07:40",
      "departureTime": "07:40",
      "distanceFromSourceKm": 1000,
      "dayOffset": 1
    }
  ]
}
```

### Key Features:
- **Popular Train Types**: Vande Bharat Express, Tejas Rajdhani, Shatabdi, Duronto, Garib Rath, Superfast, Mail/Express, Intercity, Jan Shatabdi, Sampark Kranti.
- **Multi-Route Density**: High-density corridor connectivity (e.g. 76+ trains connecting Howrah `HWH` and Mughalsarai `DDU`, 60+ trains on Delhi-Kanpur-Patna corridor, etc.).
- **Day Offset Tracking**: Realistic day progression (`dayOffset: 0, 1, 2`) for multi-day overnight journeys.
- **Realistic Distances & Timetables**: Accurate station sequence numbers, cumulative distances in kilometers, and realistic arrival/departure dwell times.

---

## 💡 Use Cases

1. **Railway Reservation & Ticketing Clones**: Model IRCTC-style seat inventory, quotas, and PNR workflows.
2. **Shortest Path & Graph Algorithms**: Dijkstra, Bellman-Ford, or A* search for train route navigation and transfers.
3. **Database Seeding**: Easily import into MongoDB (`mongoimport`), PostgreSQL/MySQL (JSONB/relational), or Neo4j (Graph DB).
4. **Machine Learning & Analytics**: Delay analysis, route optimization, transit demand modeling, and congestion analysis.

---

## 📜 License
This dataset is open-source and released under the **MIT License**. Free for personal, commercial, and educational use.
