import { VNPay } from 'vnpay';

const vnpay = new VNPay({
  tmnCode: 'F4KQG2CS',
  secureSecret: 'YAVZYYIX7RP8MD0N69KYIW7FUMC7A0SN',
  vnpayHost: 'https://sandbox.vnpayment.vn',
  testMode: true,
  hashAlgorithm: 'SHA512',
  enableLog: true,
});

export default vnpay;
