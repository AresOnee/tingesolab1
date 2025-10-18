// src/hooks/useAsync.js

import { useState, useCallback } from 'react';

/**
 * Hook personalizado para manejar operaciones asíncronas
 * Elimina código duplicado de loading/error/success en componentes
 * 
 * @example
 * const { loading, error, execute } = useAsync();
 * 
 * const handleCreate = async () => {
 *   await execute(toolService.create(formData));
 *   showSuccess('Herramienta creada exitosamente');
 * };
 */
export const useAsync = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [data, setData] = useState(null);

  const execute = useCallback(async (asyncFunction) => {
    setLoading(true);
    setError(null);
    
    try {
      const result = await asyncFunction;
      setData(result);
      return result;
    } catch (err) {
      setError(err);
      throw err; // Re-throw para que el componente pueda manejarlo
    } finally {
      setLoading(false);
    }
  }, []);

  const reset = useCallback(() => {
    setLoading(false);
    setError(null);
    setData(null);
  }, []);

  return { 
    loading, 
    error, 
    data, 
    execute,
    reset
  };
};

/**
 * Hook especializado para operaciones con feedback específico
 * Maneja múltiples operaciones simultáneas con loading individual
 * 
 * @example
 * const { loading, execute } = useAsyncOperations();
 * 
 * await execute('create', async () => {
 *   await toolService.create(data);
 * });
 * 
 * // loading.create === true durante la operación
 */
export const useAsyncOperations = () => {
  const [loading, setLoading] = useState({});
  const [errors, setErrors] = useState({});

  const execute = useCallback(async (operationName, asyncFunction) => {
    setLoading(prev => ({ ...prev, [operationName]: true }));
    setErrors(prev => ({ ...prev, [operationName]: null }));
    
    try {
      const result = await asyncFunction();
      return result;
    } catch (err) {
      setErrors(prev => ({ ...prev, [operationName]: err }));
      throw err;
    } finally {
      setLoading(prev => ({ ...prev, [operationName]: false }));
    }
  }, []);

  const isLoading = useCallback((operationName) => {
    return loading[operationName] || false;
  }, [loading]);

  const getError = useCallback((operationName) => {
    return errors[operationName];
  }, [errors]);

  const reset = useCallback((operationName) => {
    if (operationName) {
      setLoading(prev => ({ ...prev, [operationName]: false }));
      setErrors(prev => ({ ...prev, [operationName]: null }));
    } else {
      setLoading({});
      setErrors({});
    }
  }, []);

  return {
    loading,
    errors,
    execute,
    isLoading,
    getError,
    reset
  };
};

export default useAsync;
