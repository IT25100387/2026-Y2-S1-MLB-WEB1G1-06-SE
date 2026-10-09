export const adminNavigation=[
  {name:'Home',path:'/dashboard'},
  {name:'Administration',groups:[{name:'Accounts',links:[['User accounts','/dashboard/users']]}]},
  {name:'Payment',groups:[{name:'Billing',links:[['Invoices','/dashboard/invoices'],['Payments','/dashboard/payments']]}]},
  {name:'Inventory',groups:[{name:'Stock',links:[['Fuel tanks','/dashboard/inventory/fuel'],['Spare parts','/dashboard/inventory/parts']]},{name:'Suppliers',links:[['Suppliers','/dashboard/inventory/suppliers']]}]},
  {name:'Catalog & offers',groups:[{name:'Offers',links:[['Spare part offers','/dashboard/offers/spare-parts'],['Service offers','/dashboard/offers/services']]},{name:'Service catalog',links:[['Service catalog','/dashboard/offers/service-catalog']]}]},
  {name:'Team & support',groups:[{name:'Team',links:[['Staff directory','/dashboard/hr/staff'],['Shift schedules','/dashboard/hr/shifts'],['Leave requests','/dashboard/hr/leave']]},{name:'Customers & support',links:[['Support tickets','/dashboard/support/tickets'],['Customer feedback','/dashboard/support/feedback'],['Customer directory','/dashboard/customers']]}]},
  {name:'Audit logs',path:'/dashboard/audit-logs'},
];
