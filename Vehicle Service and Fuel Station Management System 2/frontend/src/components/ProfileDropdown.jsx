import AccountMenu from './AccountMenu';

export default function ProfileDropdown({handleLogout,isOpen,setIsOpen}) {
  return <AccountMenu open={isOpen} setOpen={setIsOpen} onLogout={handleLogout}/>;
}
