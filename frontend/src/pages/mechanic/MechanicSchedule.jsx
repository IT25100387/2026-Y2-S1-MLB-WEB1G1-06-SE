import { apiFetch as fetch } from '../../lib/api';
import React, { useState, useEffect } from 'react';

const MechanicSchedule = () => {
  const [selectedMechanic, setSelectedMechanic] = useState(null);
  const [mechanics, setMechanics] = useState([]);
  const [schedules, setSchedules] = useState({});
  const [isLoading, setIsLoading] = useState(true);

  // Fetch data from Java Backend API
  useEffect(() => {
    const fetchSchedules = async () => {
      try {
        const response = await fetch('/api/workshop/mechanics/schedules', {
          credentials: 'include'
        });
        
        if (response.ok) {
          const data = await response.json();
          setMechanics(data.mechanics);
          setSchedules(data.schedules);
        } else {
          console.error("Failed to fetch mechanics: Unauthorized");
        }
      } catch (error) {
        console.error("Error fetching mechanics API:", error);
      } finally {
        setIsLoading(false);
      }
    };
    
    fetchSchedules();
  }, []);

  const closeModal = () => setSelectedMechanic(null);

  return (
    <div className="p-4 sm:p-8">
      
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-2xl sm:text-3xl font-black text-white flex items-center gap-3">
          <span className="text-brand-accent">⚙️</span> Mechanic Schedule
        </h1>
        <p className="text-gray-400 text-sm mt-2">View all active mechanics and their assigned workshop schedules.</p>
      </div>

      {/* Main Table Panel */}
      <div className="glass-panel p-1 sm:p-6 rounded-2xl border border-white/5 bg-[#0f0f0f]/80 overflow-hidden">
        <div className="flex items-center gap-3 mb-6 p-4 sm:p-0">
          <span className="text-white text-lg">📋</span>
          <h2 className="text-lg font-bold text-white">Active Mechanics Roster</h2>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-white/5 border-y border-white/10">
                <th className="p-4 text-xs font-bold tracking-widest text-gray-400 uppercase">Mechanic ID</th>
                <th className="p-4 text-xs font-bold tracking-widest text-gray-400 uppercase">Name</th>
                <th className="p-4 text-xs font-bold tracking-widest text-gray-400 uppercase">Status</th>
                <th className="p-4 text-xs font-bold tracking-widest text-gray-400 uppercase text-right">Action</th>
              </tr>
            </thead>
            <tbody>
              {mechanics.map((mech) => (
                <tr key={mech.id} className="border-b border-white/5 hover:bg-white/5 transition-colors">
                  <td className="p-4 text-brand-accent font-bold">#{mech.id}</td>
                  <td className="p-4 text-white font-medium">{mech.name}</td>
                  <td className="p-4">
                    <span className={`px-3 py-1 rounded-full text-xs font-bold ${mech.status === 'Active' ? 'bg-green-500/10 text-green-500 border border-green-500/20' : 'bg-gray-500/10 text-gray-400 border border-gray-500/20'}`}>
                      {mech.status}
                    </span>
                  </td>
                  <td className="p-4 text-right">
                    <button 
                      onClick={() => setSelectedMechanic(mech)}
                      className="bg-brand-gray text-white px-4 py-2 rounded-lg font-bold text-xs hover:bg-brand-accent hover:text-black transition-colors border border-white/10 whitespace-nowrap"
                    >
                      <i aria-hidden="true" className="fa-solid fa-calendar-day mr-2"></i> View Schedule
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal Overlay */}
      {selectedMechanic && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
          <div className="glass-panel w-full max-w-2xl bg-[#121212] rounded-2xl shadow-2xl border border-white/10 flex flex-col max-h-[90vh]">
            
            <div className="p-6 border-b border-white/10 flex justify-between items-center">
              <h3 className="text-xl font-black text-white">
                <span className="text-brand-accent mr-2">📅</span> 
                Schedule for {selectedMechanic.name}
              </h3>
              <button onClick={closeModal} className="text-gray-400 hover:text-white transition-colors text-2xl leading-none">&times;</button>
            </div>

            <div className="p-6 overflow-y-auto">
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="bg-white/5 border-y border-white/10">
                      <th className="p-3 text-xs font-bold tracking-widest text-gray-400 uppercase">Date</th>
                      <th className="p-3 text-xs font-bold tracking-widest text-gray-400 uppercase">Time Slot</th>
                      <th className="p-3 text-xs font-bold tracking-widest text-gray-400 uppercase">Vehicle Plate</th>
                      <th className="p-3 text-xs font-bold tracking-widest text-gray-400 uppercase">Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {schedules[selectedMechanic.id]?.length > 0 ? (
                      schedules[selectedMechanic.id].map((s, idx) => (
                        <tr key={idx} className="border-b border-white/5">
                          <td className="p-3 text-white text-sm">{s.date}</td>
                          <td className="p-3 text-white font-bold text-sm">{s.timeSlot}</td>
                          <td className="p-3">
                            <span className="bg-brand-black border border-white/10 px-3 py-1 rounded text-xs font-mono text-gray-300">
                              {s.vehicle}
                            </span>
                          </td>
                          <td className="p-3">
                            <span className={`px-2 py-1 rounded text-[10px] font-bold uppercase tracking-wider ${
                              s.status === 'Approved' ? 'bg-blue-500/10 text-blue-400' :
                              s.status === 'In Progress' ? 'bg-yellow-500/10 text-yellow-400' :
                              'bg-gray-500/10 text-gray-400'
                            }`}>
                              {s.status}
                            </span>
                          </td>
                        </tr>
                      ))
                    ) : (
                      <tr>
                        <td colSpan="4" className="p-8 text-center text-gray-500 font-medium text-sm">
                          No active scheduled jobs for this mechanic.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            <div className="p-6 border-t border-white/10 flex justify-end">
              <button 
                onClick={closeModal}
                className="bg-white/10 text-white hover:bg-white hover:text-black transition-colors px-6 py-2 rounded-lg font-bold text-sm"
              >
                Close
              </button>
            </div>
            
          </div>
        </div>
      )}

    </div>
  );
};

export default MechanicSchedule;
