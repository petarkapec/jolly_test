import React, {useState, useEffect} from 'react';
import axios from 'axios';
import '../styles/Modal.css';
import useAuth from '../hooks/useAuth';

const Modal = ({isVisible, onClose, role}) => {
  const [newGroupName, setNewGroupName] = useState('');
  const [selectedUsers, setSelectedUsers] = useState([]);
  const [allUsers, setAllUsers] = useState([]);

  // const allUsers = [
  //   { id: 1, username: 'Alice' },
  //   { id: 2, username: 'Bob' },
  //   { id: 3, username: 'Charlie' },
  //   { id: 4, username: 'David' },
  //   { id: 5, username: 'Eve' }
  //  ]

  useEffect(() => {
    if (role !== 'Participant') {
      const fetchUsers = async () => {
        try {
          const response = await axios.get('http://localhost:8080/admin/users', { withCredentials: true });
          setAllUsers(response.data);
        } catch (error) {
          console.error('Error fetching users:', error);
        }
      };
      fetchUsers();
    }
  }, [role]);

  const handleCreateGroup = async () => {
    try {
      await axios.post('http://localhost:8080/admin/groups', {
        name: newGroupName,
        users: selectedUsers
      }, {withCredentials: true});
      onClose();
    } catch (error) {
      console.error('Error creating group:', error);
    }
  };

  const handleUserSelection = (userId) => {
    setSelectedUsers(prevSelectedUsers =>
      prevSelectedUsers.includes(userId)
        ? prevSelectedUsers.filter(id => id !== userId)
        : [...prevSelectedUsers, userId]
    );
  };

  if (!isVisible) return null;

  const handleOverlayClick = (e) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  return (
    <div className="modal-overlay" onClick={handleOverlayClick}>
      <div className="modal-content">
        <button className="modal-close-button" onClick={onClose}>X</button>
        {role === 'Participant' ? (
          <div className="participant-modal-content">
            <h2>Create a Group</h2>
            <p>To create a group, you need to be a Christmas president. Do you want to apply for that role?</p>
            <button onClick={onClose}>Apply</button>
          </div>
        ) : (
          <div className="new-group-form">
            <h2>Create New Group</h2>
            <input
              type="text"
              placeholder="Group Name"
              value={newGroupName}
              onChange={(e) => setNewGroupName(e.target.value)}
            />
            <h3>Select Users</h3>
            <ul>
              {allUsers.map(user => (
                <li key={user.id}>
                  <label>
                    <input
                      type="checkbox"
                      checked={selectedUsers.includes(user.id)}
                      onChange={() => handleUserSelection(user.id)}
                    />
                    {user.username}
                  </label>
                </li>
              ))}
            </ul>
            <button onClick={handleCreateGroup}>Create Group</button>
          </div>
        )}
      </div>
    </div>
  );
};

export default Modal;