import { daysBetween, isoDay, parseDay, shortDay } from './dates';

test('fechas locales sin desfase de zona horaria', () => {
  expect(isoDay(new Date(2025, 0, 5))).toBe('2025-01-05');
  expect(parseDay('2025-06-02').getDate()).toBe(2);
  expect(daysBetween('2025-06-02', '2025-06-08')).toBe(7);
  expect(daysBetween('2025-03-29', '2025-03-31')).toBe(3); // cambio de hora incluido
  expect(shortDay('2025-06-02')).toBe('2 jun');
});
