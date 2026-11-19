
import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { createPrescription, getPrescriptionImage } from './api';
import type { MedicinePrescription, UserMedicine } from './types';

async function prepareImage(file: File): Promise<Blob> {
  if (!['image/jpeg', 'image/png'].includes(file.type)) {
    throw new Error('Choose a JPEG or PNG image.');
  }
  const image = await createImageBitmap(file);
  try {
    const scale = Math.min(1, 1800 / Math.max(image.width, image.height));
    const canvas = document.createElement('canvas');
    canvas.width = Math.max(1, Math.round(image.width * scale));
    canvas.height = Math.max(1, Math.round(image.height * scale));
    const context = canvas.getContext('2d');
    if (!context) throw new Error('Unable to prepare the image.');
    context.fillStyle = '#fff';
    context.fillRect(0, 0, canvas.width, canvas.height);
    context.drawImage(image, 0, 0, canvas.width, canvas.height);
    return await new Promise((resolve, reject) => canvas.toBlob(
      blob => blob ? resolve(blob) : reject(new Error('Unable to prepare the image.')),
      'image/jpeg', 0.85,
    ));
  } finally {
    image.close();
  }
}

export default function MedicinePrescriptionsPanel({ prescriptions, medicines, disabled, today, onCreate }: {
  prescriptions: MedicinePrescription[];
  medicines: UserMedicine[];
  disabled: boolean;
  today: string;
  onCreate: (prescription: MedicinePrescription) => void;
}) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [image, setImage] = useState<{ url: string; id: string } | null>(null);

  useEffect(() => () => {
    if (image) URL.revokeObjectURL(image.url);
  }, [image]);

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    const photo = data.get('image');
    data.delete('image');
    setBusy(true);
    setError('');
    try {
      if (photo instanceof File && photo.size > 0) {
        data.append('image', await prepareImage(photo), 'prescription.jpg');
      }
      onCreate(await createPrescription(data));
      form.reset();
    } catch {
      setError('Unable to save the prescription. Check the details and choose a readable JPEG or PNG.');
    } finally {
      setBusy(false);
    }
  }

  async function showImage(id: string) {
    setBusy(true);
    setError('');
    try {
      setImage({ id, url: URL.createObjectURL(await getPrescriptionImage(id)) });
    } catch {
      setError('Unable to load the prescription image.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="medicine-panel">
      <h2>Prescriptions</h2>
      {error && <p className="auth-alert" role="alert">{error}</p>}
      <form onSubmit={handleCreate}>
        <fieldset disabled={disabled || busy}>
          <div className="medicine-fields">
            <label className="auth-field">Doctor name <input name="doctorName" maxLength={150} required /></label>
            <label className="auth-field">Prescription date
              <input name="prescriptionDate" type="date" defaultValue={today} required />
            </label>
          </div>
          <label className="auth-field">Private image (optional)
            <input name="image" type="file" accept="image/jpeg,image/png" />
          </label>
          <button className="auth-primary" type="submit">Save prescription</button>
        </fieldset>
      </form>
      {prescriptions.map(prescription => {
        const linked = medicines.filter(medicine => medicine.prescriptionId === prescription.id);
        return (
          <article className="medicine-item" key={prescription.id}>
            <h3>{prescription.doctorName} · {prescription.prescriptionDate}</h3>
            {linked.length ? <ul>{linked.map(medicine => (
              <li key={medicine.id}>{medicine.medicineName} ({medicine.status.toLowerCase()})</li>
            ))}</ul> : <p>No medicines linked yet.</p>}
            {prescription.hasImage && (image?.id === prescription.id ? (
              <>
                <button type="button" onClick={() => setImage(null)}>Hide image</button>
                <img className="medicine-prescription-image" src={image.url} alt="Prescription supplied by you" />
              </>
            ) : (
              <button type="button" disabled={disabled || busy} onClick={() => void showImage(prescription.id)}>
                View private image
              </button>
            ))}
          </article>
        );
      })}
    </section>
  );
}
