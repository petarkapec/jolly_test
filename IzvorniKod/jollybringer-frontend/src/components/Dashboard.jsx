import { useState, useEffect } from 'react';
import axios from 'axios';
import useAuth from '../hooks/useAuth';
import '../styles/Dashboard.css';
import Activities from "./Activities.jsx";
import Chat from "./Chat.jsx";
import CountdownTimer from "./CountdownTimer.jsx";
import Modal from './Modal.jsx';

const Dashboard = () => {
  const [isMenuVisible, setIsMenuVisible] = useState(false);
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [groups, setGroups] = useState([]);
  const { role, user, loading } = useAuth();

  useEffect(() => {
    const fetchGroups = async () => {
      try {
        const response = await axios.get(`${import.meta.env.VITE_BACKEND_URL}/groups`, { withCredentials: true });
        setGroups(response.data);
      } catch (error) {
        console.error('Error fetching groups:', error);
      }
    };

    fetchGroups();
  }, []);

  if (loading) {
    return <div>Loading...</div>;
  }

  const handleNewGroupClick = () => {
    setIsModalVisible(true);
  };

  const handleLogout = async () => {
    try {
      await axios.post(`${import.meta.env.VITE_BACKEND_URL}/logout`, {}, { withCredentials: true });
      window.location.href = '/';
    } catch (error) {
      console.error('Error logging out:', error);
    }
  };

  const handleAdminRedirect = () => {
    window.location.href = '/dashboard/admin';
  };

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <div className='logo'>Jolly Bringer</div>
        <CountdownTimer page={'Dashboard'}/>
        <div className='dashboard-header-user-data'>
          <div
            className="groups"
            onMouseEnter={() => setIsMenuVisible(true)}
            onMouseLeave={() => setIsMenuVisible(false)}
          >
            {role}
            {isMenuVisible && (
              <div className="extended-menu">
                <ul>
                  {groups.map(group => (
                    <li key={group.id}>{group.name}</li>
                  ))}
                  <li onClick={handleNewGroupClick}>+ New group</li>
                  {(role === 'Admin') && (
                    <li onClick={handleAdminRedirect}>Admin Dashboard</li>
                  )}
                </ul>
              </div>
            )}
          </div>
          <button className="dashboard-header-button" onClick={handleLogout}>Logout</button>
        </div>
      </header>
      <div className="dashboard-content">
        <Activities/>
        <Chat/>
      </div>
      {isModalVisible && <Modal isVisible={isModalVisible} onClose={() => setIsModalVisible(false)} user={user} role={role}/>}
    </div>
  );
};

export default Dashboard;