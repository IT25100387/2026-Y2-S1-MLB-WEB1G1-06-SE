import React from 'react';
import { motion } from 'framer-motion';

const CustomerWIP = ({ title }) => {
  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex flex-col items-center justify-center min-h-[60vh] text-center">
      <i aria-hidden="true" className="fa-solid fa-person-digging text-6xl text-brand-accent mb-6"></i>
      <h2 className="text-3xl font-black uppercase tracking-widest text-white mb-4">{title}</h2>
      <p className="text-gray-400 max-w-md">This module is currently being migrated to the new React interface. Please check back shortly!</p>
    </motion.div>
  );
};

export default CustomerWIP;
