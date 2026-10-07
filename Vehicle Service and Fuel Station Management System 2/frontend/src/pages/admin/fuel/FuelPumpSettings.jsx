import PumpSettings from '../../../components/PumpSettings';
import FuelPageShell from './FuelPageShell';

export default function FuelPumpSettings(){
  return <FuelPageShell section="pump-settings" title="Pump settings." description="Manage pump details, fuel tanks and operating states." requiresFeed={false}><section id="pump-settings"><PumpSettings/></section></FuelPageShell>;
}
